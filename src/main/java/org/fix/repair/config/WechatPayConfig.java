package org.fix.repair.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.CertificateVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 微信支付配置
 */
@Configuration
@ConfigurationProperties(prefix = "wxpay")
@Data
@Getter
@Validated
@Slf4j
public class WechatPayConfig {

    @Value("${wxpay.appId}")
    private String appId;

    @Value("${wxpay.mchId}")
    private String mchId;

    @Value("${wxpay.privateKeyPath}")
    private String privateKeyPath;

    @Value("${wxpay.merchantSerialNumber}")
    private String merchantSerialNumber;

    @Value("${wxpay.apiV3Key}")
    private String apiV3Key;

    @Value("${wxpay.publicKeyId:#{null}}")
    private String publicKeyId;

    @Value("${wxpay.publicKeyPath:#{null}}")
    private String publicKeyPath;

    @Value("${wxpay.notifyUrl}")
    private String notifyUrl;

    private String refundNotifyUrl;

    @Value("${wxpay.mode:prod}")
    private String mode;

    @Value("${wxpay.testAmountInCents:1}")
    private Integer testAmountInCents;

    @Value("${wxpay.appSecret}")
    private String appSecret;

    public boolean isMockMode() {
        return "mock".equalsIgnoreCase(mode);
    }

    public boolean isSandbox() {
        return "sandbox".equalsIgnoreCase(mode);
    }

    // ==================== 工具方法：读取密钥文件内容 ====================
    private String readKeyContent(String keyPath) throws IOException {
        if (keyPath == null || keyPath.isBlank()) {
            throw new IllegalArgumentException("密钥路径不能为空");
        }

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource;

        if (keyPath.startsWith("classpath:")) {
            resource = resolver.getResource(keyPath);
        } else if (keyPath.startsWith("file:")) {
            resource = resolver.getResource(keyPath);
        } else {
            // 默认为文件系统绝对/相对路径
            resource = resolver.getResource("file:" + keyPath);
        }

        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("密钥文件不存在或不可读: " + keyPath);
        }

        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ==================== 核心 Bean：微信支付配置 ====================
    @Bean
    public Config wechatPayCoreConfig() throws IOException {
        log.info("开始初始化微信支付配置，商户号: {}", mchId);

        // 必填校验
        if (mchId == null || mchId.isBlank()) {
            throw new IllegalArgumentException("wxpay.mchId 不能为空");
        }
        if (privateKeyPath == null || privateKeyPath.isBlank()) {
            throw new IllegalArgumentException("wxpay.privateKeyPath 不能为空");
        }
        if (merchantSerialNumber == null || merchantSerialNumber.isBlank()) {
            throw new IllegalArgumentException("wxpay.merchantSerialNumber 不能为空");
        }
        if (apiV3Key == null || apiV3Key.isBlank()) {
            throw new IllegalArgumentException("wxpay.apiV3Key 不能为空");
        }

        // 读取私钥内容
        String privateKeyContent = readKeyContent(privateKeyPath);
        log.info("私钥加载成功，路径: {}", privateKeyPath);

        // ========== 优先方案：RSAAutoCertificateConfig（自动下载平台证书）==========
        try {
            log.info("尝试使用 RSAAutoCertificateConfig（自动证书管理）");
            return new RSAAutoCertificateConfig.Builder()
                    .merchantId(mchId)
                    .privateKey(privateKeyContent)           // 传入字符串
                    .merchantSerialNumber(merchantSerialNumber)
                    .apiV3Key(apiV3Key)
                    .build();
        } catch (Exception e) {
            log.warn("RSAAutoCertificateConfig 初始化失败: {}", e.getMessage());
            // 继续尝试降级方案
        }

        // ========== 降级方案：RSAPublicKeyConfig（需手动提供平台公钥）==========
        if (publicKeyPath != null && !publicKeyPath.isBlank()
                && publicKeyId != null && !publicKeyId.isBlank()) {

            try {
                String publicKeyContent = readKeyContent(publicKeyPath);
                log.info("降级使用 RSAPublicKeyConfig，公钥路径: {}", publicKeyPath);

                return new RSAPublicKeyConfig.Builder()
                        .merchantId(mchId)
                        .privateKey(privateKeyContent)
                        .merchantSerialNumber(merchantSerialNumber)
                        .apiV3Key(apiV3Key)
                        .publicKeyId(publicKeyId)
                        .publicKey(publicKeyContent)
                        .build();

            } catch (Exception e) {
                log.error("RSAPublicKeyConfig 初始化也失败: {}", e.getMessage());
            }
        }

        throw new IllegalStateException(
                "微信支付配置初始化失败。\n" +
                        "推荐：仅配置 privateKeyPath + merchantSerialNumber + apiV3Key，" +
                        "使用 RSAAutoCertificateConfig 自动下载平台证书。\n" +
                        "或者：额外配置 publicKeyPath 和 publicKeyId 使用手动公钥模式。"
        );
    }



    // ==================== 服务 Bean ====================
    @Bean
    public com.wechat.pay.java.service.payments.jsapi.JsapiService jsapiService() throws IOException {
        return new com.wechat.pay.java.service.payments.jsapi.JsapiService.Builder()
                .config(wechatPayCoreConfig()).build();
    }

    @Bean
    public com.wechat.pay.java.service.refund.RefundService refundService() throws IOException {
        return new com.wechat.pay.java.service.refund.RefundService.Builder()
                .config(wechatPayCoreConfig()).build();
    }




}