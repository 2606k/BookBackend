package org.fix.repair.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.refund.RefundService;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置
 */
@Configuration
@ConfigurationProperties(prefix = "wx.pay")
@Data
public class WechatPayConfig {
    
    /**
     * 微信支付应用ID
     */
    private String appId;
    
    /**
     * 微信支付商户号
     */
    private String mchId;
    
    /**
     * 商户私钥字符串
     */
    private String privateKey;
    
    /**
     * 商户证书序列号
     */
    private String merchantSerialNumber;
    
    /**
     * 微信支付平台证书
     */
    private String apiV3Key;
    
    /**
     * 支付回调地址
     */
    private String notifyUrl;
    
    /**
     * 退款回调地址
     */
    private String refundNotifyUrl;

    @Bean
    public Config wechatPayConfig() {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .privateKey(privateKey)
                .merchantSerialNumber(merchantSerialNumber)
                .apiV3Key(apiV3Key)
                .build();
    }

    @Bean
    public JsapiService jsapiService() {
        return new JsapiService.Builder().config(wechatPayConfig()).build();
    }

    @Bean
    public RefundService refundService() {
        return new RefundService.Builder().config(wechatPayConfig()).build();
    }
}