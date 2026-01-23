package net.coding.template.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.pay")
public class WeChatPayConfig {

    private String appId;
    private String mchId;
    private String mchKey;
    private String apiKey;
    private String serialNo;
    private String certPath;
    private String notifyUrl;
    private String freezeNotifyUrl;

    // 在WeChatPayConfig类中添加
    private String apiV3Key; // API v3密钥
    private String privateKeyPath; // 商户私钥路径
    private String privateKey; // 商户私钥内容

    @Bean
    public CloseableHttpClient wechatPayHttpClient() throws Exception {
        // 加载商户证书
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(new File(certPath))) {
            keyStore.load(fis, mchId.toCharArray());
        }

        // 构建SSL上下文
        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, mchId.toCharArray())
                .build();

        // 创建HTTP客户端
        return HttpClients.custom()
                .setSSLContext(sslContext)
                .build();
    }
}
