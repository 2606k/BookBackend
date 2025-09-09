package org.fix.repair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechat.pay.java.core.notification.Notification;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.util.NonceUtil;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.PrepayResponse;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.partnerpayments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import lombok.Value;
import org.fix.repair.common.R;
import org.fix.repair.config.WechatPayConfig;
import org.fix.repair.entity.Order;
import org.fix.repair.entity.OrderItem;
import org.fix.repair.entity.books;
import org.fix.repair.mapper.OrderMapper;
import org.fix.repair.service.BooksService;
import org.fix.repair.service.OrderItemService;
import org.fix.repair.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 书籍下单
 */
@RestController
@RequestMapping("/appoint")
public class OrderController {

    private OrderService orderService;
    private OrderItemService orderItemService;
    private OrderMapper orderMapper;
    private WechatPayConfig wechatPayConfig;
    private BooksService booksService;
    private JsapiService jsapiService;

    public OrderController(OrderService orderService, OrderItemService orderItemService) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.orderMapper = orderMapper;
        this.wechatPayConfig = wechatPayConfig;
        this.booksService = booksService;
        this.jsapiService = jsapiService;
    }

    String appId = wechatPayConfig.getAppId();
    String mchId = wechatPayConfig.getMchId();
    String notifyUrl = wechatPayConfig.getNotifyUrl();

    /**
     * 创建订单并发起支付
     * 前端传参：{ openid, name, phone, address, remark, bookItems: [{bookId, quantity}] }
     */
    @PostMapping("/create")
    public R<Map<String, String>> createOrder(@RequestBody Map<String, Object> params) {
        // 提取参数
        String openid = (String) params.get("openid");
        String name = (String) params.get("name");
        String phone = (String) params.get("phone");
        String address = (String) params.get("address");
        String remark = (String) params.get("remark");
        List<Map<String, Object>> bookItems = (List<Map<String, Object>>) params.get("bookItems");

        // 创建订单
        Order order = new Order();
        order.setOpenid(openid);
        order.setName(name);
        order.setPhone(phone);
        order.setAddress(address);
        order.setRemark(remark);
        order.setOutTradeNo(UUID.randomUUID().toString().replace("-", "")); // 生成唯一订单号
        order.setStatus("待支付"); // 初始状态
        order.setCreatedat(new Date());

        // 校验参数
        if (openid == null || name == null || phone == null || address == null || bookItems == null || bookItems.isEmpty()) {
            return R.error("参数不完整");
        }
        // 计算总数量和总金额
        int totalNum = 0;
        int totalMoney = 0;
        for (Map<String, Object> item : bookItems) {

            Long bookId = Long.valueOf(item.get("bookId").toString());
            books book = booksService.getBook(bookId);
            Integer quantity = Integer.valueOf(item.get("quantity").toString());
            Integer price = book.getPrice(); // 假设 Service 有方法获取价格
            totalNum += quantity;
            totalMoney += price * quantity;

            // 保存 OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId()); // 将在保存 order 后设置
            orderItem.setBookId(bookId);
            orderItem.setBookName(book.getBookName()); // 假设获取书籍名
            orderItem.setQuantity(quantity);
            orderItem.setPrice(price);
            orderItemService.save(orderItem);
        }
        order.setNum(totalNum);
        order.setMoney(totalMoney);

        // 保存订单
        orderService.save(order);

        // 更新 OrderItem 的 orderId
        for (Map<String, Object> item : bookItems) {
            Long bookId = Long.valueOf(item.get("bookId").toString());
            OrderItem orderItem = orderItemService.getOne(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getBookId, bookId)
                            .eq(OrderItem::getOrderId, null) // 假设刚插入的 OrderItem
            );
            if (orderItem != null) {
                orderItem.setOrderId(order.getId());
                orderItemService.updateById(orderItem);
            }
        }

        // 调用微信统一下单 API
        PrepayRequest prepayRequest = new PrepayRequest();
        prepayRequest.setAppid(appId); // 从配置注入
        prepayRequest.setMchid(mchId); // 从配置注入
        prepayRequest.setDescription("图书商城订单");
        prepayRequest.setOutTradeNo(order.getOutTradeNo());
        prepayRequest.setNotifyUrl(notifyUrl); // 从配置注入
        Amount amount = new Amount();
        amount.setTotal(totalMoney); // 总金额，单位：分
        prepayRequest.setAmount(amount);
        Payer payer = new Payer();
        payer.setOpenid(openid);
        prepayRequest.setAppid(payer);

        try {
            // 发起预支付请求
            PrepayResponse response = jsapiService.prepay(prepayRequest);

            // 生成小程序支付参数
            String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonceStr = NonceUtil.createNonce();
            String packageStr = "prepay_id=" + response.getPrepayId();
            String signType = "RSA";
            String paySign = PaySign.createSign(
                    wechatPayConfig.getAppid(),
                    timeStamp,
                    nonceStr,
                    packageStr,
                    wechatPayConfig.getApiV3Key()
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
            throw new RuntimeException("发起支付失败: " + e.getMessage());
        }

        return R.ok(payParams);
    }

    /**
     * 微信支付回调
     */
    @PostMapping("/notify")
    public String payNotify(@RequestBody String notifyData) {
        try {
            // 解析回调通知
            NotificationParser parser = new NotificationParser(wechatPayConfig);
            Notification notification = parser.parse(notifyData, Notification.class);

            // 验证通知
            if ("TRANSACTION.SUCCESS".equals(notification.getEventType())) {
                // 获取订单信息
                String outTradeNo = notification.getResource().getOutTradeNo();
                Order order = orderService.getOne(
                        new LambdaQueryWrapper<Order>().eq(Order::getOutTradeNo, outTradeNo)
                );
                if (order != null && "待支付".equals(order.getStatus())) {
                    order.setTransactionId(notification.getResource().getTransactionId());
                    order.setStatus("0"); // 已支付
                    order.setPayTime(new Date());
                    orderService.updateById(order);
                }
            }

            // 返回成功响应
            return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
        } catch (Exception e) {
            // 返回失败响应
            return "{\"code\":\"FAIL\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 申请退款（用户端）
     */
    @PostMapping("/refund/apply")
    public R<String> applyRefund(@RequestParam Long orderId, @RequestParam(required = false) String reason) {
        Order order = orderService.getById(orderId);
        if (order == null || !"0".equals(order.getStatus())) {
            return R.error("订单不可退款");
        }

        order.setStatus("1"); // 申请退款
        order.setRemark(order.getRemark() + " 退款原因: " + (reason != null ? reason : ""));
        orderService.updateById(order);

        return R.ok("申请退款成功，等待审核");
    }

    /**
     * 订单退款回调
     */
    @PostMapping("/refund/notify")
    public String refundNotify(@RequestBody String notifyData) {
        try {
            NotificationParser parser = new NotificationParser(wechatPayConfig());
            Notification notification = parser.parse(notifyData, Notification.class);
            if ("REFUND.SUCCESS".equals(notification.getEventType())) {
                String outTradeNo = notification.getResource().getOutTradeNo();
                Order order = orderService.getOne(
                        new LambdaQueryWrapper<Order>().eq(Order::getOutTradeNo, outTradeNo)
                );
                if (order != null && "1".equals(order.getStatus())) {
                    order.setStatus("2"); // 已退款
                    orderService.updateById(order);
                }
            }
            return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
        } catch (Exception e) {
            return "{\"code\":\"FAIL\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 订单退款（管理员端）
     */
    @PostMapping("/refund/execute")
    public R<String> executeRefund(@RequestParam Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null || !"1".equals(order.getStatus())) {
            return R.error("订单不可执行退款");
        }

        RefundService refundService = new RefundService.Builder().config(wechatPayConfig()).build();
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOutTradeNo(order.getOutTradeNo());
        refundRequest.setOutRefundNo(UUID.randomUUID().toString().replace("-", "")); // 退款单号
        refundRequest.setNotifyUrl(notifyUrl); // 退款回调地址
        RefundRequest.Amount amount = new RefundRequest.Amount();
        amount.setRefund(order.getMoney()); // 退款金额
        amount.setTotal(order.getMoney()); // 原订单金额
        amount.setCurrency("CNY");
        refundRequest.setAmount(amount);

        Refund refund = refundService.refund(refundRequest);
        if ("SUCCESS".equals(refund.getStatus())) {
            order.setStatus("2"); // 已退款
            orderService.updateById(order);
            return R.ok("退款成功");
        } else {
            return R.error("退款失败: " + refund.getStatus());
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
    public R<Page<Order>> listOrders(@RequestBody Map<String, Object> orderMap,
                                     @RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer size) {
        Page<Order> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        // 按创建时间降序
        wrapper.orderByDesc(Order::getCreatedat);

        // 动态查询条件
        if (orderMap.get("openid") != null) {
            wrapper.eq(Order::getOpenid, orderMap.get("openid"));
        }
        if (orderMap.get("address") != null) {
            wrapper.like(Order::getAddress, orderMap.get("address"));
        }
        if (orderMap.get("phone") != null) {
            wrapper.eq(Order::getPhone, orderMap.get("phone"));
        }
        if (orderMap.get("name") != null) {
            wrapper.like(Order::getName, orderMap.get("name"));
        }

        // 执行分页查询
        orderService.page(pageObj, wrapper);

        // 为每个订单加载 OrderItem 列表
        for (Order order : pageObj.getRecords()) {
            List<OrderItem> items = orderItemService.listByOrderId(order.getId());
            order.setOrderItems(items); // 设置到 transient 字段
        }

        // 返回分页对象，包含订单和订单项
        return R.ok(pageObj);
    }

}
