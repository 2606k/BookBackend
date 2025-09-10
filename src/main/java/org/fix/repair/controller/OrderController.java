package org.fix.repair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechat.pay.java.core.notification.Notification;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.util.NonceUtil;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundRequest;
import com.wechat.pay.java.core.notification.RequestParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.fix.repair.common.PaySign;
import org.fix.repair.common.R;
import org.fix.repair.config.WechatPayConfig;
import org.fix.repair.entity.Order;
import org.fix.repair.entity.OrderItem;
import org.fix.repair.entity.books;
import org.fix.repair.mapper.OrderMapper;
import org.fix.repair.service.BooksService;
import org.fix.repair.service.OrderItemService;
import org.fix.repair.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 书籍下单控制器
 * @author tangxin
 */
@Slf4j
@RestController
@RequestMapping("/appoint")
public class OrderController {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderItemService orderItemService;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private WechatPayConfig wechatPayConfig;
    
    @Autowired
    private BooksService booksService;
    
    @Autowired
    private JsapiService jsapiService;

    /**
     * 创建订单并发起支付
     * 前端传参：{ openid, name, phone, address, remark, bookItems: [{bookId, quantity}] }
     */
    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    public R<Map<String, String>> createOrder(@RequestBody Map<String, Object> params) {
        try {
            // 1. 参数校验
            R<String> validationResult = validateOrderParams(params);
            if (!validationResult.getCode().equals(200)) {
                return R.error(validationResult.getMsg());
            }

            // 2. 提取参数
            String openid = (String) params.get("openid");
            String name = (String) params.get("name");
            String phone = (String) params.get("phone");
            String address = (String) params.get("address");
            String remark = (String) params.get("remark");
            List<Map<String, Object>> bookItems = (List<Map<String, Object>>) params.get("bookItems");

            // 3. 校验书籍库存和存在性
            R<String> stockResult = validateBookStock(bookItems);
            if (!stockResult.getCode().equals(200)) {
                return R.error(stockResult.getMsg());
            }

            // 4. 创建订单
            Order order = createOrderEntity(openid, name, phone, address, remark);
            
            // 5. 计算订单金额和创建订单项
            R<Map<String, Object>> orderResult = calculateOrderAmount(order, bookItems);
            if (!orderResult.getCode().equals(200)) {
                return R.error(orderResult.getMsg());
            }
            
            order = (Order) orderResult.getData().get("order");
            List<OrderItem> orderItems = (List<OrderItem>) orderResult.getData().get("orderItems");

            // 6. 保存订单和订单项
            orderService.save(order);
            for (OrderItem orderItem : orderItems) {
                orderItem.setOrderId(order.getId());
                orderItemService.save(orderItem);
            }

            // 7. 发起微信支付
            return initiateWechatPayment(order);

        } catch (Exception e) {
            log.error("创建订单失败", e);
            return R.error("创建订单失败: " + e.getMessage());
        }
    }

    /**
     * 微信支付回调
     */
    @PostMapping("/notify")
    @Transactional(rollbackFor = Exception.class)
    public String payNotify(@RequestBody String notifyData,
                           @RequestHeader("Wechatpay-Signature") String signature,
                           @RequestHeader("Wechatpay-Timestamp") String timestamp,
                           @RequestHeader("Wechatpay-Nonce") String nonce,
                           @RequestHeader("Wechatpay-Serial") String serial) {
        try {
            log.info("收到微信支付回调: {}", notifyData);
            
            // 解析回调通知
            NotificationParser parser = new NotificationParser(wechatPayConfig.wechatPayConfig());
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serial)
                    .nonce(nonce)
                    .signature(signature)
                    .timestamp(timestamp)
                    .body(notifyData)
                    .build();
            
            Notification notification = parser.parse(requestParam);

            // 验证通知
            if ("TRANSACTION.SUCCESS".equals(notification.getEventType())) {
                // 获取订单信息 - 从解密后的资源中获取
                String resourceJson = notification.getResource().toString();
                // 解析JSON获取订单信息
                ObjectMapper mapper = new ObjectMapper();
                JsonNode resourceNode = mapper.readTree(resourceJson);
                
                String outTradeNo = resourceNode.get("out_trade_no").asText();
                String transactionId = resourceNode.get("transaction_id").asText();
                
                Order order = orderService.getOne(
                        new LambdaQueryWrapper<Order>().eq(Order::getOutTradeNo, outTradeNo)
                );
                
                if (order != null && "待支付".equals(order.getStatus())) {
                    // 更新订单状态
                    order.setTransactionId(transactionId);
                    order.setStatus("0"); // 已支付
                    order.setPayTime(new Date());
                    orderService.updateById(order);
                    
                    // 扣减库存
                    updateBookStock(order.getId());
                    
                    log.info("订单支付成功，订单号: {}", outTradeNo);
                } else {
                    log.warn("订单状态异常或订单不存在，订单号: {}, 状态: {}", 
                            outTradeNo, order != null ? order.getStatus() : "null");
                }
            }

            // 返回成功响应
            return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
        } catch (Exception e) {
            log.error("处理微信支付回调失败", e);
            // 返回失败响应
            return "{\"code\":\"FAIL\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 扣减库存
     */
    private void updateBookStock(Long orderId) {
        try {
            List<OrderItem> orderItems = orderItemService.listByOrderId(orderId);
            for (OrderItem item : orderItems) {
                books book = booksService.getBook(item.getBookId());
                if (book != null) {
                    book.setStock(book.getStock() - item.getQuantity());
                    booksService.updateById(book);
                    log.info("扣减库存成功，书籍: {}, 扣减数量: {}", book.getBookName(), item.getQuantity());
                }
            }
        } catch (Exception e) {
            log.error("扣减库存失败", e);
        }
    }

    /**
     * 申请退款（用户端）
     */
    @PostMapping("/refund/apply")
    @Transactional(rollbackFor = Exception.class)
    public R<String> applyRefund(@RequestParam Long orderId, @RequestParam(required = false) String reason) {
        try {
            Order order = orderService.getById(orderId);
            if (order == null) {
                return R.error("订单不存在");
            }
            if (!"0".equals(order.getStatus())) {
                return R.error("订单状态不允许退款，当前状态: " + order.getStatus());
            }

            order.setStatus("1"); // 申请退款
            order.setRemark(order.getRemark() + " 退款原因: " + (reason != null ? reason : ""));
            orderService.updateById(order);

            log.info("用户申请退款，订单ID: {}, 原因: {}", orderId, reason);
            return R.ok("申请退款成功，等待审核");
        } catch (Exception e) {
            log.error("申请退款失败", e);
            return R.error("申请退款失败: " + e.getMessage());
        }
    }

    /**
     * 订单退款回调
     */
    @PostMapping("/refund/notify")
    @Transactional(rollbackFor = Exception.class)
    public String refundNotify(@RequestBody String notifyData,
                              @RequestHeader("Wechatpay-Signature") String signature,
                              @RequestHeader("Wechatpay-Timestamp") String timestamp,
                              @RequestHeader("Wechatpay-Nonce") String nonce,
                              @RequestHeader("Wechatpay-Serial") String serial) {
        try {
            log.info("收到微信退款回调: {}", notifyData);
            
            NotificationParser parser = new NotificationParser(wechatPayConfig.wechatPayConfig());
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serial)
                    .nonce(nonce)
                    .signature(signature)
                    .timestamp(timestamp)
                    .body(notifyData)
                    .build();
            
            Notification notification = parser.parse(requestParam);
            
            if ("REFUND.SUCCESS".equals(notification.getEventType())) {
                // 获取订单信息 - 从解密后的资源中获取
                String resourceJson = notification.getResource().toString();
                // 解析JSON获取订单信息
                ObjectMapper mapper = new ObjectMapper();
                JsonNode resourceNode = mapper.readTree(resourceJson);
                
                String outTradeNo = resourceNode.get("out_trade_no").asText();
                
                Order order = orderService.getOne(
                        new LambdaQueryWrapper<Order>().eq(Order::getOutTradeNo, outTradeNo)
                );
                
                if (order != null && "1".equals(order.getStatus())) {
                    order.setStatus("2"); // 已退款
                    orderService.updateById(order);
                    
                    // 恢复库存
                    restoreBookStock(order.getId());
                    
                    log.info("退款成功，订单号: {}", outTradeNo);
                }
            }
            return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
        } catch (Exception e) {
            log.error("处理退款回调失败", e);
            return "{\"code\":\"FAIL\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 恢复库存
     */
    private void restoreBookStock(Long orderId) {
        try {
            List<OrderItem> orderItems = orderItemService.listByOrderId(orderId);
            for (OrderItem item : orderItems) {
                books book = booksService.getBook(item.getBookId());
                if (book != null) {
                    book.setStock(book.getStock() + item.getQuantity());
                    booksService.updateById(book);
                    log.info("恢复库存成功，书籍: {}, 恢复数量: {}", book.getBookName(), item.getQuantity());
                }
            }
        } catch (Exception e) {
            log.error("恢复库存失败", e);
        }
    }

    /**
     * 订单退款（管理员端）
     */
    @PostMapping("/refund/execute")
    @Transactional(rollbackFor = Exception.class)
    public R<String> executeRefund(@RequestParam Long orderId) {
        try {
            Order order = orderService.getById(orderId);
            if (order == null) {
                return R.error("订单不存在");
            }
            if (!"1".equals(order.getStatus())) {
                return R.error("订单状态不允许执行退款，当前状态: " + order.getStatus());
            }

            RefundService refundService = new RefundService.Builder().config(wechatPayConfig.wechatPayConfig()).build();
            RefundRequest refundRequest = new RefundRequest();
            refundRequest.setOutTradeNo(order.getOutTradeNo());
            refundRequest.setOutRefundNo(UUID.randomUUID().toString().replace("-", ""));
            refundRequest.setNotifyUrl(wechatPayConfig.getNotifyUrl());
            
            RefundRequest.Amount amount = new RefundRequest.Amount();
            amount.setRefund(order.getMoney());
            amount.setTotal(order.getMoney());
            amount.setCurrency("CNY");
            refundRequest.setAmount(amount);

            Refund refund = refundService.refund(refundRequest);
            if ("SUCCESS".equals(refund.getStatus())) {
                order.setStatus("2"); // 已退款
                orderService.updateById(order);
                
                // 恢复库存
                restoreBookStock(orderId);
                
                log.info("管理员执行退款成功，订单ID: {}", orderId);
                return R.ok("退款成功");
            } else {
                return R.error("退款失败: " + refund.getStatus());
            }
        } catch (Exception e) {
            log.error("执行退款失败", e);
            return R.error("执行退款失败: " + e.getMessage());
        }
    }


    /**
     * 查询订单
     * - 如果 openid 不为空：查询个人订单（微信用户端）
     * - 如果 openid 为空：查询全部订单（管理端）
     * 支持分页：page 当前页，size 每页大小
     * 管理端可以不停刷新此接口（不带 openid）
     */
    @GetMapping("/list")
    public R<Page<Order>> listOrders(@RequestParam(required = false) String openid,
                                     @RequestParam(required = false) String address,
                                     @RequestParam(required = false) String phone,
                                     @RequestParam(required = false) String name,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer size) {
        try {
            Page<Order> pageObj = new Page<>(page, size);
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            
            // 按创建时间降序
            wrapper.orderByDesc(Order::getCreatedat);

            // 动态查询条件
            if (openid != null && !openid.trim().isEmpty()) {
                wrapper.eq(Order::getOpenid, openid);
            }
            if (address != null && !address.trim().isEmpty()) {
                wrapper.like(Order::getAddress, address);
            }
            if (phone != null && !phone.trim().isEmpty()) {
                wrapper.eq(Order::getPhone, phone);
            }
            if (name != null && !name.trim().isEmpty()) {
                wrapper.like(Order::getName, name);
            }
            if (status != null && !status.trim().isEmpty()) {
                wrapper.eq(Order::getStatus, status);
            }

            // 执行分页查询
            orderService.page(pageObj, wrapper);

            // 为每个订单加载 OrderItem 列表
            for (Order order : pageObj.getRecords()) {
                List<OrderItem> items = orderItemService.listByOrderId(order.getId());
                order.setOrderItems(items);
            }

            log.info("查询订单列表，条件: openid={}, page={}, size={}, 结果数量: {}", 
                    openid, page, size, pageObj.getRecords().size());
            
            return R.ok(pageObj);
        } catch (Exception e) {
            log.error("查询订单列表失败", e);
            return R.error("查询订单列表失败: " + e.getMessage());
        }
    }

    /**
     * 校验订单参数
     */
    private R<String> validateOrderParams(Map<String, Object> params) {
        String openid = (String) params.get("openid");
        String name = (String) params.get("name");
        String phone = (String) params.get("phone");
        String address = (String) params.get("address");
        List<Map<String, Object>> bookItems = (List<Map<String, Object>>) params.get("bookItems");

        if (openid == null || openid.trim().isEmpty()) {
            return R.error("openid不能为空");
        }
        if (name == null || name.trim().isEmpty()) {
            return R.error("姓名不能为空");
        }
        if (phone == null || phone.trim().isEmpty()) {
            return R.error("手机号不能为空");
        }
        if (address == null || address.trim().isEmpty()) {
            return R.error("地址不能为空");
        }
        if (bookItems == null || bookItems.isEmpty()) {
            return R.error("商品列表不能为空");
        }

        // 校验手机号格式
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return R.error("手机号格式不正确");
        }

        return R.ok("参数校验通过");
    }

    /**
     * 校验书籍库存和存在性
     */
    private R<String> validateBookStock(List<Map<String, Object>> bookItems) {
        for (Map<String, Object> item : bookItems) {
            try {
                Long bookId = Long.valueOf(item.get("bookId").toString());
                Integer quantity = Integer.valueOf(item.get("quantity").toString());

                if (quantity <= 0) {
                    return R.error("商品数量必须大于0");
                }

                books book = booksService.getBook(bookId);
                if (book == null) {
                    return R.error("商品不存在，ID: " + bookId);
                }

                if (book.getStock() < quantity) {
                    return R.error("商品库存不足，商品: " + book.getBookName() + "，库存: " + book.getStock());
                }

            } catch (NumberFormatException e) {
                return R.error("商品ID或数量格式不正确");
            }
        }
        return R.ok("库存校验通过");
    }

    /**
     * 创建订单实体
     */
    private Order createOrderEntity(String openid, String name, String phone, String address, String remark) {
        Order order = new Order();
        order.setOpenid(openid);
        order.setName(name);
        order.setPhone(phone);
        order.setAddress(address);
        order.setRemark(remark);
        order.setOutTradeNo(UUID.randomUUID().toString().replace("-", ""));
        order.setStatus("待支付");
        order.setCreatedat(new Date());
        return order;
    }

    /**
     * 计算订单金额和创建订单项
     */
    private R<Map<String, Object>> calculateOrderAmount(Order order, List<Map<String, Object>> bookItems) {
        try {
            int totalNum = 0;
            int totalMoney = 0;
            List<OrderItem> orderItems = new ArrayList<>();

            for (Map<String, Object> item : bookItems) {
                Long bookId = Long.valueOf(item.get("bookId").toString());
                Integer quantity = Integer.valueOf(item.get("quantity").toString());

                books book = booksService.getBook(bookId);
                Integer price = book.getPrice();

                totalNum += quantity;
                totalMoney += price * quantity;

                // 创建订单项
                OrderItem orderItem = new OrderItem();
                orderItem.setBookId(bookId);
                orderItem.setBookName(book.getBookName());
                orderItem.setQuantity(quantity);
                orderItem.setPrice(price);
                orderItems.add(orderItem);
            }

            order.setNum(totalNum);
            order.setMoney(totalMoney);

            Map<String, Object> result = new HashMap<>();
            result.put("order", order);
            result.put("orderItems", orderItems);

            return R.ok(result);
        } catch (Exception e) {
            log.error("计算订单金额失败", e);
            return R.error("计算订单金额失败: " + e.getMessage());
        }
    }

    /**
     * 发起微信支付
     */
    private R<Map<String, String>> initiateWechatPayment(Order order) {
        try {
            // 创建预支付请求
            PrepayRequest prepayRequest = new PrepayRequest();
            prepayRequest.setAppid(wechatPayConfig.getAppId());
            prepayRequest.setMchid(wechatPayConfig.getMchId());
            prepayRequest.setDescription("图书商城订单-" + order.getOutTradeNo());
            prepayRequest.setOutTradeNo(order.getOutTradeNo());
            prepayRequest.setNotifyUrl(wechatPayConfig.getNotifyUrl());

            // 设置金额
            Amount amount = new Amount();
            amount.setTotal(order.getMoney());
            amount.setCurrency("CNY");
            prepayRequest.setAmount(amount);

            // 设置支付者信息
            Payer payer = new Payer();
            payer.setOpenid(order.getOpenid());
            prepayRequest.setPayer(payer);

            // 发起预支付请求
            PrepayResponse response = jsapiService.prepay(prepayRequest);

            // 生成小程序支付参数
            String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonceStr = NonceUtil.createNonce();
            String packageStr = "prepay_id=" + response.getPrepayId();
            String signType = "RSA";

            // 生成签名
            String paySign = PaySign.createSign(
                    wechatPayConfig.getAppId(),
                    timeStamp,
                    nonceStr,
                    packageStr,
                    wechatPayConfig.getPrivateKeyPath()
            );

            Map<String, String> payParams = new HashMap<>();
            payParams.put("appId", wechatPayConfig.getAppId());
            payParams.put("timeStamp", timeStamp);
            payParams.put("nonceStr", nonceStr);
            payParams.put("package", packageStr);
            payParams.put("signType", signType);
            payParams.put("paySign", paySign);

            return R.ok(payParams);

        } catch (Exception e) {
            log.error("发起微信支付失败", e);
            return R.error("发起支付失败: " + e.getMessage());
        }
    }

}
