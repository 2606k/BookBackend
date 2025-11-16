package org.fix.repair.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.fix.repair.config.WechatPayConfig;
import org.fix.repair.config.WechatUtil;
import org.fix.repair.entity.Order;
import org.fix.repair.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/pay")
@Slf4j
public class PayCallbackController {

    @Autowired
    private Config wechatPayCoreConfig;

    @Autowired
    private WechatUtil wechatUtil;

    @Autowired
    private WechatPayConfig wechatPayConfig;

    // TODO: 注入你的订单服务
     @Autowired
     private OrderService orderService;

    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> payCallback(
            @RequestBody String body,
            @RequestHeader("Wechatpay-Serial") String serialNumber,
            @RequestHeader("Wechatpay-Signature") String signature,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Timestamp") String timestamp
    ) {
        log.info("【支付回调】serial: {}", serialNumber);

        try {
            // 1. 使用 SDK 验签+解密
            NotificationParser parser = new NotificationParser((NotificationConfig) wechatPayCoreConfig);
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serialNumber)
                    .nonce(nonce)
                    .signature(signature)
                    .timestamp(timestamp)
                    .body(body)
                    .build();

            Transaction transaction = parser.parse(requestParam, Transaction.class);

            // ⚠️ 加个状态校验
            if (transaction.getTradeState() != Transaction.TradeStateEnum.SUCCESS) {
                log.warn("非成功状态: {}", transaction.getTradeState());
                return ResponseEntity.ok(Map.of("code", "SUCCESS")); // 也要返回成功，否则微信会重复推送
            }

            String outTradeNo = transaction.getOutTradeNo();
            String transactionId = transaction.getTransactionId();
            Transaction.TradeStateEnum state = transaction.getTradeState();

            log.info("【支付回调】订单: {}, 状态: {}", outTradeNo, state);

            // 2. 只处理支付成功
            if (state != Transaction.TradeStateEnum.SUCCESS) {
                return ResponseEntity.ok(Map.of("code", "SUCCESS"));
            }

            // 3. 幂等性检查（防止重复处理）
            // if (orderService.isPaid(outTradeNo)) {
            //     log.info("订单已处理: {}", outTradeNo);
            //     return ResponseEntity.ok(Map.of("code", "SUCCESS"));
            // }

            // 4. 更新订单状态
            // orderService.markAsPaid(outTradeNo, transactionId);

            // 5. 发货通知
//            notifyWechatDelivery(outTradeNo, transactionId);

            boolean confirm = notifyWechatDeliveryForSelfPickup(outTradeNo, transactionId);
            if (!confirm) {
                log.error("【发货失败】{}", outTradeNo);
                return ResponseEntity.status(500)
                        .body(Map.of("code", "FAIL", "message", "系统异常"));
            }

            return ResponseEntity.ok(Map.of("code", "SUCCESS"));

        } catch (Exception e) {
            log.error("【支付回调】异常", e);
            // ⚠️ 注意：返回非 200 或 code!=SUCCESS，微信会重复推送（最多 10 次）
            return ResponseEntity.status(500)
                    .body(Map.of("code", "FAIL", "message", "系统异常"));
        }
    }

    private void notifyWechatDelivery(String outTradeNo, String transactionId) {
        try {
            String accessToken = wechatUtil.getAccessToken();
            String url = "https://api.weixin.qq.com/wxa/sec/order/notify_deliver?access_token=" + accessToken;

            Map<String, Object> body = Map.of(
                    "order_key", Map.of(
                            "out_trade_no", outTradeNo,
                            "transaction_id", transactionId
                    ),
                    "logistics_type", 2
            );

            String result = cn.hutool.http.HttpUtil.post(url, JSON.toJSONString(body));
            JSONObject json = JSON.parseObject(result);

            if (json.getInteger("errcode") == 0) {
                log.info("【发货成功】{}", outTradeNo);
            } else {
                log.error("【发货失败】{}: {}", json.getInteger("errcode"), json.getString("errmsg"));
                // TODO: 加入重试队列
            }
        } catch (Exception e) {
            log.error("【发货异常】{}", outTradeNo, e);
            // TODO: 加入重试队列
        }
    }

    private boolean notifyWechatDeliveryForSelfPickup(String outTradeNo,String transactionId) {
        if (outTradeNo == null || outTradeNo.isEmpty()) {
            log.warn("【自提发货】订单号为空");
            return  false;
        }

        try {
            String accessToken = wechatUtil.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                log.error("【自提发货】access_token 获取失败");
                return  false;
            }

//            String url = "https://api.weixin.qq.com/wxa/sec/order/upload_shipping_info?access_token=" + accessToken;

            Order order = orderService.getUserOpenidByOutTradeNo(outTradeNo);

            // === 3. 构造请求体 ===
            JSONObject orderKey = new JSONObject();
            orderKey.put("order_number_type", 1); // 2使用 transaction_id
            orderKey.put("transaction_id", transactionId);
            orderKey.put("out_trade_no", outTradeNo);
            orderKey.put("mchid",wechatPayConfig.getMchId());

            JSONObject shipItem = new JSONObject();
            shipItem.put("item_desc",  "图书"); // 防空

            JSONArray shippingList = new JSONArray();
            shippingList.add(shipItem);

            String uploadTime = Instant.now()
                    .atZone(ZoneId.of("Asia/Shanghai"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));

            JSONObject payer = new JSONObject();
            payer.put("openid", order.getOpenid());

            JSONObject requestBody = new JSONObject();
            requestBody.put("order_key", orderKey);
            if("1".equals(order.getDeliveryType())) {
                requestBody.put("logistics_type", 2);
            } else {
                requestBody.put("logistics_type", 4);
            }// 用户自提
            requestBody.put("delivery_mode", 1);  // 统一发货
            requestBody.put("is_all_delivered", true);
            requestBody.put("upload_time", uploadTime);
            requestBody.put("payer", payer);
            requestBody.put("shipping_list", shippingList);

            String jsonBody = requestBody.toJSONString();
            log.info("【自提发货请求JSON】{}", jsonBody);

            // === 4. 发送 HTTP 请求 ===
            String url = "https://api.weixin.qq.com/wxa/sec/order/upload_shipping_info?access_token=" + accessToken;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            // === 5. 处理响应 ===
            log.info("【微信原始响应】{}", response.getBody());

            JSONObject respJson = JSONObject.parseObject(response.getBody());
            int errcode = respJson.getIntValue("errcode");

            if (errcode == 0) {
                log.info("【自提发货成功】订单: {}", outTradeNo);
                // 可选：标记订单为“已通知微信发货”
                order.setStatus("4");
                order.setTransactionId(transactionId);
                orderService.update().eq("out_trade_no", outTradeNo).update(order);
                return true;
                // orderService.markWechatDeliveryNotified(outTradeNo);
            } else {
                String errmsg = respJson.getString("errmsg");
                log.error("【自提发货失败】订单: {}, errcode: {}, errmsg: {}", outTradeNo, errcode, errmsg);
                return false;
                // 可考虑重试机制 or 告警
            }

        } catch (Exception e) {
            log.error("【自提发货异常】订单: {}", outTradeNo, e);
            return false;
        }
    }



}
