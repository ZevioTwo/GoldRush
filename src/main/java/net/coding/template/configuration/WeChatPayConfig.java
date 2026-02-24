package net.coding.template.configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
        if (certPath == null || certPath.isBlank()) {
            log.warn("wechat.pay.cert-path 未配置，使用默认HttpClient启动（微信支付将不可用）");
            return HttpClients.createDefault();
        }

        File certFile = new File(certPath);
        if (!certFile.exists()) {
            log.warn("wechat.pay.cert-path 文件不存在: {}，使用默认HttpClient启动（微信支付将不可用）", certFile.getAbsolutePath());
            return HttpClients.createDefault();
        }

        // 加载商户证书
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(certFile)) {
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
