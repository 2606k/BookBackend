package org.fix.repair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
// 暂时注释掉复杂的通知解析，使用简化的JSON处理
// import com.wechat.pay.java.core.notification.Notification;
// import com.wechat.pay.java.core.notification.NotificationParser;
// import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.core.util.NonceUtil;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final OrderMapper orderMapper;
    private final WechatPayConfig wechatPayConfig;
    private final BooksService booksService;
    private final JsapiService jsapiService;
    private final RefundService refundService;

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
     * 查询订单
     */
    @GetMapping("/queryOrder")
    public R<String> queryOrder(@RequestParam String outTradeNo) {
        try {
            log.info("查询订单: {}", outTradeNo);
            // TODO: 实现微信支付订单查询
            return R.ok("订单查询功能待实现");
        } catch (Exception e) {
            log.error("查询订单失败", e);
            return R.error("查询订单失败: " + e.getMessage());
        }
    }

    /**
     * 关闭订单
     */
    @GetMapping("/closeOrder/{outTradeNo}")
    public R<String> closeOrder(@PathVariable String outTradeNo) {
        try {
            // TODO: 实现微信支付订单关闭
            
            // 更新本地订单状态
            Order order = orderService.getOne(
                    new LambdaQueryWrapper<Order>().eq(Order::getOutTradeNo, outTradeNo)
            );
            if (order != null && "待支付".equals(order.getStatus())) {
                order.setStatus("已关闭");
                orderService.updateById(order);
                log.info("订单已关闭，订单号: {}", outTradeNo);
            }
            
            return R.ok("订单关闭成功");
        } catch (Exception e) {
            log.error("关闭订单失败", e);
            return R.error("关闭订单失败: " + e.getMessage());
        }
    }

    /**
     * 微信支付回调
     */
    @PostMapping("/notify")
    @Transactional(rollbackFor = Exception.class)
    public String payNotify(HttpServletRequest request, @RequestBody String notifyData) {
        try {
            log.info("收到微信支付回调: {}", notifyData);
            
            // 获取请求头
            String signature = request.getHeader("Wechatpay-Signature");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String serial = request.getHeader("Wechatpay-Serial");

            // 验证必要的请求头
            if (signature == null || timestamp == null || nonce == null || serial == null) {
                log.error("微信支付回调缺少必要的请求头");
                return buildFailResponse("缺少必要的请求头");
            }
            
            // 简化处理，直接解析JSON数据
            // TODO: 在生产环境中需要验证签名和解密数据
            try {
                // 直接处理支付回调数据
                return handlePaymentSuccessSimple(notifyData);
            } catch (Exception e) {
                log.error("解析支付通知失败", e);
                return buildFailResponse("解析通知失败");
            }

        } catch (Exception e) {
            log.error("处理微信支付回调失败", e);
            return buildFailResponse(e.getMessage());
        }
    }

    /**
     * 简化的支付成功处理逻辑
     */
    private String handlePaymentSuccessSimple(String notifyData) {
        try {
            log.info("处理支付回调数据: {}", notifyData);
            
            // 解析JSON获取订单信息
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(notifyData);
            
            // 检查事件类型
            String eventType = rootNode.path("event_type").asText();
            if (!"TRANSACTION.SUCCESS".equals(eventType)) {
                log.warn("收到非成功支付事件: {}", eventType);
                return buildSuccessResponse();
            }
            
            // 获取资源数据（这里简化处理，实际需要解密）
            JsonNode resourceNode = rootNode.path("resource");
            String algorithm = resourceNode.path("algorithm").asText();
            String ciphertext = resourceNode.path("ciphertext").asText();
            String nonce = resourceNode.path("nonce").asText();
            String associatedData = resourceNode.path("associated_data").asText();
            
            // TODO: 这里需要实现解密逻辑，暂时模拟处理
            log.info("收到支付成功通知，算法: {}, 密文长度: {}", algorithm, ciphertext.length());
            
            // 简化返回成功（实际项目中需要完整的解密和验证逻辑）
            return buildSuccessResponse();
            
        } catch (Exception e) {
            log.error("处理支付回调失败", e);
            return buildFailResponse(e.getMessage());
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
                    int newStock = book.getStock() - item.getQuantity();
                    if (newStock < 0) {
                        log.warn("库存不足，书籍: {}, 当前库存: {}, 需要扣减: {}", 
                                book.getBookName(), book.getStock(), item.getQuantity());
                        newStock = 0; // 防止库存为负数
                    }
                    book.setStock(newStock);
                    booksService.updateById(book);
                    log.info("扣减库存成功，书籍: {}, 扣减数量: {}, 剩余库存: {}", 
                            book.getBookName(), item.getQuantity(), newStock);
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
    public R<String> applyRefund(@RequestParam Long orderId, 
                                @RequestParam(required = false) String reason) {
        try {
            Order order = orderService.getById(orderId);
            if (order == null) {
                return R.error("订单不存在");
            }
            if (!"0".equals(order.getStatus())) {
                return R.error("订单状态不允许退款，当前状态: " + getStatusText(order.getStatus()));
            }

            order.setStatus("1"); // 申请退款
            String newRemark = order.getRemark() != null ? order.getRemark() : "";
            newRemark += " [退款原因: " + (reason != null ? reason : "用户申请退款") + "]";
            order.setRemark(newRemark);
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
    public String refundNotify(HttpServletRequest request, @RequestBody String notifyData) {
        try {
            log.info("收到微信退款回调: {}", notifyData);
            
            // 获取请求头
            String signature = request.getHeader("Wechatpay-Signature");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String serial = request.getHeader("Wechatpay-Serial");

            // 验证必要的请求头
            if (signature == null || timestamp == null || nonce == null || serial == null) {
                log.error("微信退款回调缺少必要的请求头");
                return buildFailResponse("缺少必要的请求头");
            }
            
            // 简化处理，直接解析JSON数据
            // TODO: 在生产环境中需要验证签名和解密数据
            try {
                // 直接处理退款回调数据
                return handleRefundSuccessSimple(notifyData);
            } catch (Exception e) {
                log.error("解析退款通知失败", e);
                return buildFailResponse("解析退款通知失败");
            }
            
        } catch (Exception e) {
            log.error("处理退款回调失败", e);
            return buildFailResponse(e.getMessage());
        }
    }

    /**
     * 简化的退款成功处理逻辑
     */
    private String handleRefundSuccessSimple(String notifyData) {
        try {
            log.info("处理退款回调数据: {}", notifyData);
            
            // 解析JSON获取订单信息
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(notifyData);
            
            // 检查事件类型
            String eventType = rootNode.path("event_type").asText();
            if (!eventType.startsWith("REFUND.")) {
                log.warn("收到非退款事件: {}", eventType);
                return buildSuccessResponse();
            }
            
            if ("REFUND.SUCCESS".equals(eventType)) {
                // 获取资源数据（这里简化处理，实际需要解密）
                JsonNode resourceNode = rootNode.path("resource");
                String algorithm = resourceNode.path("algorithm").asText();
                String ciphertext = resourceNode.path("ciphertext").asText();
                
                // TODO: 这里需要实现解密逻辑，暂时模拟处理
                log.info("收到退款成功通知，算法: {}, 密文长度: {}", algorithm, ciphertext.length());
                
                // 简化返回成功（实际项目中需要完整的解密和验证逻辑）
                return buildSuccessResponse();
            } else if ("REFUND.ABNORMAL".equals(eventType)) {
                log.error("退款异常");
                return buildSuccessResponse();
            } else if ("REFUND.CLOSED".equals(eventType)) {
                log.warn("退款关闭");
                return buildSuccessResponse();
            } else {
                log.warn("收到未知退款事件: {}", eventType);
                return buildSuccessResponse();
            }
            
        } catch (Exception e) {
            log.error("处理退款回调失败", e);
            return buildFailResponse(e.getMessage());
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
                    log.info("恢复库存成功，书籍: {}, 恢复数量: {}, 当前库存: {}", 
                            book.getBookName(), item.getQuantity(), book.getStock());
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
    public R<String> executeRefund(@RequestParam Long orderId, 
                                  @RequestParam(required = false) String reason) {
        try {
            Order order = orderService.getById(orderId);
            if (order == null) {
                return R.error("订单不存在");
            }
            if (!"1".equals(order.getStatus()) && !"0".equals(order.getStatus())) {
                return R.error("订单状态不允许执行退款，当前状态: " + getStatusText(order.getStatus()));
            }

            // 构建退款请求
            CreateRequest refundRequest = new CreateRequest();
            refundRequest.setOutTradeNo(order.getOutTradeNo());
            refundRequest.setOutRefundNo("refund_" + UUID.randomUUID().toString().replace("-", ""));
            refundRequest.setReason(reason != null ? reason : "管理员执行退款");
            refundRequest.setNotifyUrl(wechatPayConfig.getRefundNotifyUrl());
            
            // 设置退款金额
            com.wechat.pay.java.service.refund.model.AmountReq amount = new com.wechat.pay.java.service.refund.model.AmountReq();
            amount.setRefund((long) order.getMoney());
            amount.setTotal((long) order.getMoney());
            amount.setCurrency("CNY");
            refundRequest.setAmount(amount);

            // 发起退款请求
            Refund refund = refundService.create(refundRequest);
            
            log.info("退款请求已发起，退款单号: {}, 状态: {}, 订单号: {}", 
                    refund.getOutRefundNo(), refund.getStatus(), order.getOutTradeNo());

            // 更新订单状态为申请退款（等待微信回调确认）
            if ("0".equals(order.getStatus())) {
                order.setStatus("1"); // 申请退款状态，等待微信回调确认
                String newRemark = order.getRemark() != null ? order.getRemark() : "";
                newRemark += " [管理员执行退款: " + (reason != null ? reason : "管理员操作") + "]";
                order.setRemark(newRemark);
                orderService.updateById(order);
            }

            return R.ok("退款请求已发起，退款单号: " + refund.getOutRefundNo());
            
        } catch (Exception e) {
            log.error("执行退款失败，订单ID: {}", orderId, e);
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
        order.setOutTradeNo("order_" + UUID.randomUUID().toString().replace("-", ""));
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
            String nonceStr = NonceUtil.createNonce(32);
            String packageStr = "prepay_id=" + response.getPrepayId();
            String signType = "RSA";

            // 生成签名
            String paySign = PaySign.createSign(
                    wechatPayConfig.getAppId(),
                    timeStamp,
                    nonceStr,
                    packageStr,
                    wechatPayConfig.getPrivateKey()
            );

            Map<String, String> payParams = new HashMap<>();
            payParams.put("appId", wechatPayConfig.getAppId());
            payParams.put("timeStamp", timeStamp);
            payParams.put("nonceStr", nonceStr);
            payParams.put("package", packageStr);
            payParams.put("signType", signType);
            payParams.put("paySign", paySign);

            log.info("发起微信支付成功，订单号: {}, prepay_id: {}", order.getOutTradeNo(), response.getPrepayId());
            return R.ok(payParams);

        } catch (Exception e) {
            log.error("发起微信支付失败，订单号: {}", order.getOutTradeNo(), e);
            return R.error("发起支付失败: " + e.getMessage());
        }
    }

    /**
     * 构建成功响应
     */
    private String buildSuccessResponse() {
        return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
    }

    /**
     * 构建失败响应
     */
    private String buildFailResponse(String message) {
        return "{\"code\":\"FAIL\",\"message\":\"" + message + "\"}";
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(String status) {
        switch (status) {
            case "待支付": return "待支付";
            case "0": return "已支付";
            case "1": return "申请退款";
            case "2": return "已退款";
            default: return "未知状态";
        }
    }
}