package org.fix.repair.common;

import com.wechat.pay.java.core.util.PemUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

/**
 * 微信支付签名工具类
 * @author tangxin
 */
@Slf4j
public class PaySign {
    
    /**
     * 生成小程序支付签名
     * @param appId 小程序appId
     * @param timeStamp 时间戳
     * @param nonceStr 随机字符串
     * @param packageStr 扩展字段
     * @param privateKey 商户私钥
     * @return 签名字符串
     */
    public static String createSign(String appId, String timeStamp, String nonceStr, 
                                   String packageStr, String privateKey) {
        try {
            // 构建签名字符串
            String signStr = appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageStr + "\n";
            
            // 加载私钥
            PrivateKey key = PemUtil.loadPrivateKey(privateKey);
            
            // 创建签名对象
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(signStr.getBytes(StandardCharsets.UTF_8));
            
            // 生成签名
            byte[] signBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signBytes);
            
        } catch (Exception e) {
            log.error("生成支付签名失败", e);
            throw new RuntimeException("生成支付签名失败: " + e.getMessage());
        }
    }
}
