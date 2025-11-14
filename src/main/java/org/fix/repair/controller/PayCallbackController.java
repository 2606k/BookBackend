package org.fix.repair.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.fix.repair.config.WechatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/pay")
@Slf4j
public class PayCallbackController {

    @Autowired
    private Config wechatPayCoreConfig;

    @Autowired
    private WechatUtil wechatUtil;

    // TODO: 注入你的订单服务
    // @Autowired
    // private OrderService orderService;

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

            notifyWechatDeliveryForSelfPickup(outTradeNo,transactionId);

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

    private void notifyWechatDeliveryForSelfPickup(String outTradeNo,String transactionId) {
        if (outTradeNo == null || outTradeNo.isEmpty()) {
            log.warn("【自提发货】订单号为空");
            return;
        }

        try {
            String accessToken = wechatUtil.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                log.error("【自提发货】access_token 获取失败");
                return;
            }

            String url = "https://api.weixin.qq.com/wxa/sec/order/upload_shipping_info?access_token=" + accessToken;

            JSONObject body = new JSONObject();
            body.put("order_id", outTradeNo);
            body.put("delivery_type", 4);
            body.put("waybill_id", "SELF_" + outTradeNo);
            body.put("delivery_company", "");

            String jsonBody = body.toJSONString();
            log.info("【请求JSON】{}", jsonBody);

            // 使用 Java 原生 HttpClient
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String result = response.body();

            log.info("【微信原始响应】{}", result);

            JSONObject resp = JSON.parseObject(result);
            if (resp == null) {
                log.error("【自提发货】响应非JSON格式，原始内容: {}", result);
                return;
            }

            Integer errcode = resp.getInteger("errcode");
            if (errcode != null && errcode == 0) {
                log.info("【自提发货成功】订单: {}", outTradeNo);
            } else {
                String errmsg = resp.getString("errmsg");
                String rid = resp.getString("rid"); // 现在应该能拿到 rid 了
                log.error("【自提发货失败】订单: {}, errcode: {}, errmsg: {}, rid: {}",
                        outTradeNo, errcode, errmsg, rid);
            }
        } catch (Exception e) {
            log.error("【自提发货异常】订单: {}", outTradeNo, e);
        }
    }



}
