package org.fix.repair.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.refund.RefundService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class WechatPayConfig {

    @Value("${wxpay.appid}")
    private String appId;

    @Value("${wxpay.mch-id}")
    private String mchId;

    @Value("${wxpay.mch-serial-no}")
    private String mchSerialNo;

    @Value("${wxpay.mch-private-key-file-path}")
    private String privateKeyPath;

    @Value("${wxpay.api-v3-key}")
    private String apiV3Key;

    @Value("${wxpay.notify-url}")
    private String notifyUrl;

    @Value("${wxpay.refund-notify-url:}")
    private String refundNotifyUrl;

    @Bean
    public Config wechatPayConfig() {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .privateKeyFromPath(privateKeyPath)
                .merchantSerialNumber(mchSerialNo)
                .apiV3Key(apiV3Key)
                .build();
    }

    @Bean
    public JsapiService jsapiService(Config config) {
        return new JsapiService.Builder().config(config).build();
    }

    @Bean
    public RefundService refundService(Config config) {
        return new RefundService.Builder().config(config).build();
    }

    /**
     * 获取私钥文件路径
     */
    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    /**
     * 获取API v3密钥
     */
    public String getApiV3Key() {
        return apiV3Key;
    }

    /**
     * 获取退款回调URL
     */
    public String getRefundNotifyUrl() {
        return refundNotifyUrl != null && !refundNotifyUrl.isEmpty() 
            ? refundNotifyUrl 
            : notifyUrl.replace("/notify", "/refund/notify");
    }
}
