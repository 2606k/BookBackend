package org.fix.repair.config;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WechatUtil {

    @Autowired
    private WechatPayConfig wechatPayConfig;

    private String accessToken;
    private long expireTime;

    public synchronized String getAccessToken() throws Exception {
        if (System.currentTimeMillis() < expireTime) {
            return accessToken;
        }

        String appId = wechatPayConfig.getAppId();
        String secret = wechatPayConfig.getAppSecret(); // 必须有！

        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + secret;
        String resp = HttpUtil.get(url);
        JSONObject json = JSON.parseObject(resp);

        if (json.containsKey("access_token")) {
            accessToken = json.getString("access_token");
            expireTime = System.currentTimeMillis() + (json.getLong("expires_in") - 200) * 1000;
            return accessToken;
        } else {
            throw new RuntimeException("获取access_token失败: " + resp);
        }
    }
}
