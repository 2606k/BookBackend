package org.fix.repair.common;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * 微信支付签名工具类
 */
public class PaySign {

    /**
     * 生成微信支付签名
     */
    public static String createSign(String appId, String timeStamp, String nonceStr, 
                                  String packageStr, String privateKeyPath) throws Exception {
        // 构造签名串
        String message = appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageStr + "\n";
        
        // 加载私钥
        PrivateKey privateKey = loadPrivateKey(privateKeyPath);
        
        // 生成签名
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes("UTF-8"));
        
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * 加载私钥
     */
    private static PrivateKey loadPrivateKey(String privateKeyPath) throws Exception {
        String privateKeyContent = "";
        
        try {
            if (privateKeyPath.startsWith("classpath:")) {
                // 从 classpath 加载
                String path = privateKeyPath.substring(10);
                ClassPathResource resource = new ClassPathResource(path);
                privateKeyContent = readFromInputStream(resource.getInputStream());
            } else {
                // 从文件系统加载
                java.nio.file.Path path = java.nio.file.Paths.get(privateKeyPath);
                privateKeyContent = new String(java.nio.file.Files.readAllBytes(path));
            }
            
            // 处理私钥内容
            privateKeyContent = privateKeyContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            
            return keyFactory.generatePrivate(spec);
            
        } catch (Exception e) {
            throw new Exception("加载私钥失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从输入流读取内容
     */
    private static String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
} 