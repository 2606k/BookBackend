package org.fix.repair.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.refund.RefundService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 微信支付配置
 */
@Configuration
@ConfigurationProperties(prefix = "wxpay")
@Data
@Validated  // 启用 Bean Validation
@Slf4j
@ConditionalOnProperty(name = "wxpay.enabled", havingValue = "true", matchIfMissing = false)
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
     * 商户私钥文件路径（classpath: 或 file: 前缀）
     */
    private String privateKeyPath;  // 改名为 path，避免与字符串混淆

    /**
     * 商户证书序列号
     */
    private String merchantSerialNumber;

    /**
     * APIv3 密钥
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
    public Config wechatPayCoreConfig() throws IOException {
        // 验证配置
        log.info("微信支付配置加载开始，商户号: {}, 序列号: {}", mchId, merchantSerialNumber);
        
        // 检查私钥文件是否存在
        if (privateKeyPath != null && !privateKeyPath.isEmpty()) {
            Path keyPath = Paths.get(privateKeyPath);
            if (!Files.exists(keyPath)) {
                log.error("私钥文件不存在: {}", privateKeyPath);
                throw new IllegalArgumentException("私钥文件不存在: " + privateKeyPath);
            }
        }
        
        // 使用 privateKeyFromPath 直接传入路径字符串，让 SDK 自己处理
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .privateKeyFromPath(privateKeyPath)  // 直接使用配置的路径
                .merchantSerialNumber(merchantSerialNumber)
                .apiV3Key(apiV3Key)
                .build();
    }

    @Bean
    public JsapiService jsapiService() throws IOException {
        return new JsapiService.Builder().config(wechatPayCoreConfig()).build();
    }

    @Bean
    public RefundService refundService() throws IOException {
        return new RefundService.Builder().config(wechatPayCoreConfig()).build();
    }
}