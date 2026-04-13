package net.coding.template.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WeChatService {

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.app-secret}")
    private String appSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 获取微信openid和session_key
     */
    public Map<String, String> getOpenidByCode(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session" +
                "?appid=" + appId +
                "&secret=" + appSecret +
                "&js_code=" + code +
                "&grant_type=authorization_code";

        try {
            String response = restTemplate.getForObject(url, String.class);
            log.info("微信登录响应: {}", response);

            JSONObject json = JSON.parseObject(response);

            if (json.containsKey("errcode")) {
                log.error("微信登录失败: {}", json.getString("errmsg"));
                throw new RuntimeException("微信登录失败: " + json.getString("errmsg"));
            }

            Map<String, String> result = new HashMap<>();
            result.put("openid", json.getString("openid"));
            result.put("sessionKey", json.getString("session_key"));
            result.put("unionid", json.getString("unionid"));

            return result;
        } catch (Exception e) {
            log.error("调用微信接口失败", e);
            throw new RuntimeException("微信服务暂时不可用");
        }
    }

    /**
     * 解密用户信息（如果需要）
     */
    public JSONObject decryptUserInfo(String encryptedData, String sessionKey, String iv) {
        // 这里需要实现微信解密算法
        // 可以使用微信官方提供的WXBizDataCrypt
        // 为了简化，这里返回模拟数据
        JSONObject userInfo = new JSONObject();
        userInfo.put("nickName", "微信用户");
        userInfo.put("avatarUrl", "");
        return userInfo;
    }
}
