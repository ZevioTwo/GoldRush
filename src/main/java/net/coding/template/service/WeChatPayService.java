package net.coding.template.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.coding.template.configuration.WeChatPayConfig;
import net.coding.template.entity.po.PaymentOrder;
import net.coding.template.exception.BusinessException;
import net.coding.template.mapper.PaymentOrderMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class WeChatPayService {

    private static final String API_BASE_URL = "https://api.mch.weixin.qq.com";
    private static final ZoneId WECHAT_ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private WeChatPayConfig weChatPayConfig;

    @Resource
    private CloseableHttpClient wechatPayHttpClient;

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    private volatile PrivateKey merchantPrivateKey;
    private volatile PublicKey platformPublicKey;

    /**
     * 统一下单（JSAPI）
     */
    public Map<String, String> unifiedOrder(PaymentOrder paymentOrder, String openid) {
        validateJsapiConfig(openid);

        try {
            JSONObject params = new JSONObject();
            params.put("appid", weChatPayConfig.getAppId());
            params.put("mchid", weChatPayConfig.getMchId());
            params.put("description", getOrderDescription(paymentOrder.getOrderType()));
            params.put("out_trade_no", paymentOrder.getWxOutTradeNo());
            params.put("notify_url", resolveNotifyUrl(paymentOrder));
            params.put("time_expire", formatWechatTime(paymentOrder.getExpireTime()));

            JSONObject amount = new JSONObject();
            amount.put("total", convertFen(paymentOrder.getAmount()));
            amount.put("currency", "CNY");
            params.put("amount", amount);

            JSONObject payer = new JSONObject();
            payer.put("openid", openid);
            params.put("payer", payer);

            JSONObject response = executePost("/v3/pay/transactions/jsapi", params);
            String prepayId = response.getString("prepay_id");
            if (!StringUtils.hasText(prepayId)) {
                log.error("微信支付下单失败，缺少 prepay_id: {}", response);
                throw new BusinessException(500, "微信支付下单失败");
            }

            paymentOrder.setWxPrepayId(prepayId);
            paymentOrderMapper.updateById(paymentOrder);

            return generateJsapiParams(prepayId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信支付下单异常: orderNo={}", paymentOrder.getOrderNo(), e);
            throw new BusinessException(500, "微信支付服务异常");
        }
    }

    /**
     * 查询商户订单
     */
    public JSONObject queryOrderByOutTradeNo(String outTradeNo) {
        validateMerchantRequestConfig();

        try {
            String apiPath = "/v3/pay/transactions/out-trade-no/"
                    + urlEncode(outTradeNo)
                    + "?mchid="
                    + urlEncode(weChatPayConfig.getMchId());
            return executeGet(apiPath);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询微信支付订单异常: outTradeNo={}", outTradeNo, e);
            throw new BusinessException(500, "查询微信支付订单失败");
        }
    }

    /**
     * 资金冻结（预授权）
     */
    public Map<String, String> freezeDeposit(PaymentOrder paymentOrder, String openid) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("appid", weChatPayConfig.getAppId());
            params.put("mchid", weChatPayConfig.getMchId());
            params.put("out_order_no", paymentOrder.getWxOutTradeNo());
            params.put("out_request_no", generateRequestNo());
            params.put("description", "契约押金冻结");

            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("total", convertFen(paymentOrder.getAmount()));
            amountMap.put("currency", "CNY");
            params.put("amount", amountMap);

            Map<String, String> payerMap = new HashMap<>();
            payerMap.put("openid", openid);
            params.put("payer", payerMap);
            params.put("notify_url", weChatPayConfig.getFreezeNotifyUrl());

            JSONObject response = executePost("/v3/payscore/permissions", params);
            if (response.containsKey("authorization_code")) {
                Map<String, String> result = new HashMap<>();
                result.put("authorization_code", response.getString("authorization_code"));
                result.put("freeze_contract_id", response.getString("authorization_code"));
                return result;
            }

            log.error("资金冻结失败: {}", response);
            return tryAlternativeFreeze(paymentOrder, openid);
        } catch (Exception e) {
            log.error("资金冻结异常", e);
            throw new BusinessException(500, "资金冻结服务异常");
        }
    }

    /**
     * 备选冻结方案（仍保留旧逻辑）
     */
    private Map<String, String> tryAlternativeFreeze(PaymentOrder paymentOrder, String openid) {
        log.info("尝试使用备选冻结方案: orderNo={}, openid={}", paymentOrder.getOrderNo(), openid);

        Map<String, String> result = new HashMap<>();
        result.put("freeze_contract_id", "ALT_" + System.currentTimeMillis());
        result.put("warning", "使用备选冻结方案，资金将进入平台中间账户");
        return result;
    }

    /**
     * 解冻资金
     */
    public boolean unfreezeDeposit(PaymentOrder paymentOrder) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("appid", weChatPayConfig.getAppId());
            params.put("mchid", weChatPayConfig.getMchId());
            params.put("out_order_no", paymentOrder.getWxOutTradeNo());
            params.put("out_request_no", generateRequestNo());
            params.put("reason", "契约完成，解冻押金");

            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("total", convertFen(paymentOrder.getAmount()));
            amountMap.put("currency", "CNY");
            params.put("amount", amountMap);

            String url = String.format("/v3/payscore/permissions/%s/terminate", paymentOrder.getFreezeContractId());
            JSONObject response = executePost(url, params);
            return response.containsKey("terminate_time");
        } catch (Exception e) {
            log.error("解冻资金异常", e);
            return false;
        }
    }

    /**
     * 扣除违约金
     */
    public boolean deductPenalty(PaymentOrder paymentOrder) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("appid", weChatPayConfig.getAppId());
            params.put("mchid", weChatPayConfig.getMchId());
            params.put("out_trade_no", paymentOrder.getWxOutTradeNo());
            params.put("description", "违约金扣除");

            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("total", convertFen(paymentOrder.getAmount()));
            amountMap.put("currency", "CNY");
            params.put("amount", amountMap);

            JSONObject response;
            if (paymentOrder.getFreezeContractId() != null
                    && paymentOrder.getFreezeContractId().startsWith("PSCORE_")) {
                params.put("authorization_code", paymentOrder.getFreezeContractId());
                response = executePost("/v3/payscore/serviceorder/direct-complete", params);
            } else {
                response = executePost("/v3/pay/transactions/jsapi", params);
            }

            return response.containsKey("transaction_id");
        } catch (Exception e) {
            log.error("扣除违约金异常", e);
            return false;
        }
    }

    /**
     * 验证支付回调签名
     */
    public boolean verifyNotification(String timestamp, String nonce, String body, String signature) {
        if (!StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(body)
                || !StringUtils.hasText(signature)) {
            return false;
        }

        try {
            PublicKey publicKey = getPlatformPublicKey();
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signature));
        } catch (BusinessException e) {
            log.error("微信支付回调验签失败: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("验证签名异常", e);
            return false;
        }
    }

    /**
     * 处理支付成功回调（保留兼容入口）
     */
    public Map<String, String> handlePaymentNotify(String notifyBody) {
        JSONObject notifyData = JSON.parseObject(notifyBody);

        Map<String, String> result = new HashMap<>();
        result.put("out_trade_no", notifyData.getString("out_trade_no"));
        result.put("transaction_id", notifyData.getString("transaction_id"));
        result.put("trade_state", notifyData.getString("trade_state"));
        result.put("success_time", notifyData.getString("success_time"));
        return result;
    }

    /**
     * 解密微信支付回调数据
     */
    public String decryptNotifyData(String ciphertext, String nonce, String associatedData) {
        try {
            byte[] apiV3Key = weChatPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8);
            byte[] cipherBytes = Base64.getDecoder().decode(ciphertext);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec keySpec = new SecretKeySpec(apiV3Key, "AES");

            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
            if (associatedData != null) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }

            byte[] decryptedBytes = cipher.doFinal(cipherBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.error("Base64解码失败: ciphertext={}", ciphertext, e);
            throw new BusinessException(400, "密文格式错误");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            log.error("AES-GCM算法不支持", e);
            throw new BusinessException(500, "解密算法不支持");
        } catch (InvalidKeyException e) {
            log.error("解密密钥无效", e);
            throw new BusinessException(500, "解密密钥错误");
        } catch (InvalidAlgorithmParameterException e) {
            log.error("解密参数错误", e);
            throw new BusinessException(400, "解密参数错误");
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            log.error("解密失败，可能是密钥错误或数据被篡改", e);
            throw new BusinessException(400, "解密失败，数据可能被篡改");
        } catch (Exception e) {
            log.error("解密回调数据异常", e);
            throw new BusinessException(500, "解密回调数据失败");
        }
    }

    /**
     * 微信支付退款接口
     */
    public boolean refundPayment(PaymentOrder originalOrder, PaymentOrder refundOrder) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("transaction_id", originalOrder.getWxTransactionId());
            params.put("out_refund_no", refundOrder.getWxOutTradeNo());
            params.put("reason", "契约取消退款");

            Map<String, Object> amount = new HashMap<>();
            amount.put("total", convertFen(originalOrder.getAmount()));
            amount.put("refund", convertFen(refundOrder.getAmount()));
            amount.put("currency", "CNY");
            params.put("amount", amount);

            JSONObject response = executePost("/v3/refund/domestic/refunds", params);
            return response.containsKey("refund_id");
        } catch (Exception e) {
            log.error("微信退款接口调用异常: orderNo={}", originalOrder.getOrderNo(), e);
            return false;
        }
    }

    private Map<String, String> generateJsapiParams(String prepayId) {
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = generateNonceStr();
        String packageStr = "prepay_id=" + prepayId;

        String signStr = weChatPayConfig.getAppId() + "\n"
                + timeStamp + "\n"
                + nonceStr + "\n"
                + packageStr + "\n";

        Map<String, String> params = new HashMap<>();
        params.put("timeStamp", timeStamp);
        params.put("nonceStr", nonceStr);
        params.put("package", packageStr);
        params.put("signType", "RSA");
        params.put("paySign", signWithSHA256(signStr));
        return params;
    }

    private JSONObject executePost(String apiPath, Map<String, ?> params) throws Exception {
        JSONObject jsonObject = new JSONObject();
        if (params != null) {
            for (Map.Entry<String, ?> entry : params.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
        }
        return executePost(apiPath, jsonObject);
    }

    private JSONObject executePost(String apiPath, JSONObject params) throws Exception {
        String requestBody = params == null ? "" : params.toJSONString();
        HttpPost httpPost = new HttpPost(API_BASE_URL + apiPath);
        httpPost.addHeader("Authorization", "WECHATPAY2-SHA256-RSA2048 "
                + generateAuthorization(apiPath, "POST", requestBody));
        httpPost.addHeader("Accept", "application/json");
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
        return executeRequest(httpPost);
    }

    private JSONObject executeGet(String apiPath) throws Exception {
        HttpGet httpGet = new HttpGet(API_BASE_URL + apiPath);
        httpGet.addHeader("Authorization", "WECHATPAY2-SHA256-RSA2048 "
                + generateAuthorization(apiPath, "GET", ""));
        httpGet.addHeader("Accept", "application/json");
        return executeRequest(httpGet);
    }

    private JSONObject executeRequest(HttpRequestBase request) throws IOException {
        try (CloseableHttpResponse response = wechatPayHttpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() == null
                    ? ""
                    : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            if (statusCode < 200 || statusCode >= 300) {
                log.error("微信支付接口调用失败: status={}, uri={}, body={}",
                        statusCode, request.getURI(), responseBody);
                throw buildWechatPayException(statusCode, responseBody);
            }

            if (!StringUtils.hasText(responseBody)) {
                return new JSONObject();
            }
            return JSON.parseObject(responseBody);
        }
    }

    private String generateAuthorization(String apiPath, String method, String body) throws Exception {
        validateMerchantRequestConfig();

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = generateNonceStr();
        String message = method + "\n"
                + apiPath + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + body + "\n";

        String signature = signWithSHA256(message);
        return String.format(
                "mchid=\"%s\",nonce_str=\"%s\",timestamp=\"%s\",serial_no=\"%s\",signature=\"%s\"",
                weChatPayConfig.getMchId(),
                nonce,
                timestamp,
                weChatPayConfig.getSerialNo(),
                signature
        );
    }

    private String generateNonceStr() {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        return nonce.length() > 32 ? nonce.substring(0, 32) : nonce;
    }

    private String generateRequestNo() {
        return "REQ" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    private String signWithSHA256(String data) {
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(getMerchantPrivateKey());
            signer.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            log.error("微信支付签名失败", e);
            throw new BusinessException(500, "微信支付签名失败");
        }
    }

    private void validateJsapiConfig(String openid) {
        validateMerchantRequestConfig();
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException(400, "未获取到微信用户标识，请重新登录后再试");
        }
        if (!StringUtils.hasText(resolveNotifyUrl(null))) {
            throw new BusinessException(500, "未配置微信支付回调地址");
        }
    }

    private void validateMerchantRequestConfig() {
        if (!StringUtils.hasText(weChatPayConfig.getAppId())) {
            throw new BusinessException(500, "未配置微信支付 appId");
        }
        if (!StringUtils.hasText(weChatPayConfig.getMchId())) {
            throw new BusinessException(500, "未配置微信支付商户号");
        }
        if (!StringUtils.hasText(weChatPayConfig.getSerialNo())) {
            throw new BusinessException(500, "未配置微信支付证书序列号");
        }
        getMerchantPrivateKey();
    }

    private String getOrderDescription(String orderType) {
        switch (orderType) {
            case "SERVICE_FEE":
                return "平台服务费";
            case "DEPOSIT_FREEZE":
                return "契约押金冻结";
            case "VIP_PAYMENT":
                return "VIP会员购买";
            case "ARBITRATION_FEE":
                return "仲裁加急费";
            case "CREDIT_RECHARGE":
                return "信誉分充值";
            default:
                return "订单支付";
        }
    }

    private String resolveNotifyUrl(PaymentOrder paymentOrder) {
        if (paymentOrder != null && StringUtils.hasText(paymentOrder.getNotifyUrl())) {
            return paymentOrder.getNotifyUrl();
        }
        return weChatPayConfig.getNotifyUrl();
    }

    private String formatWechatTime(LocalDateTime time) {
        if (time == null) {
            return LocalDateTime.now().plusMinutes(30)
                    .atZone(WECHAT_ZONE)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        return time.atZone(WECHAT_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private int convertFen(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private BusinessException buildWechatPayException(int statusCode, String responseBody) {
        String message = "微信支付接口调用失败";
        if (StringUtils.hasText(responseBody)) {
            try {
                JSONObject jsonObject = JSON.parseObject(responseBody);
                if (StringUtils.hasText(jsonObject.getString("message"))) {
                    message = jsonObject.getString("message");
                }
            } catch (Exception e) {
                log.warn("解析微信支付错误响应失败: {}", responseBody, e);
            }
        }
        return new BusinessException(statusCode, message);
    }

    private PrivateKey getMerchantPrivateKey() {
        if (merchantPrivateKey != null) {
            return merchantPrivateKey;
        }

        synchronized (this) {
            if (merchantPrivateKey == null) {
                String pem = loadPemContent(
                        weChatPayConfig.getPrivateKey(),
                        weChatPayConfig.getPrivateKeyPath(),
                        "商户私钥"
                );
                merchantPrivateKey = parsePrivateKey(pem);
            }
        }
        return merchantPrivateKey;
    }

    private PublicKey getPlatformPublicKey() {
        if (platformPublicKey != null) {
            return platformPublicKey;
        }

        synchronized (this) {
            if (platformPublicKey == null) {
                String pem = loadPemContent(
                        weChatPayConfig.getPlatformPublicKey(),
                        weChatPayConfig.getPlatformPublicKeyPath(),
                        "微信支付平台公钥"
                );
                platformPublicKey = parsePublicKey(pem);
            }
        }
        return platformPublicKey;
    }

    private String loadPemContent(String inlinePem, String filePath, String configName) {
        try {
            if (StringUtils.hasText(inlinePem)) {
                return inlinePem;
            }
            if (StringUtils.hasText(filePath)) {
                return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new BusinessException(500, configName + "读取失败");
        }
        throw new BusinessException(500, "未配置" + configName);
    }

    private PrivateKey parsePrivateKey(String pemContent) {
        try {
            String normalizedPem = normalizePem(pemContent)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "");
            byte[] encoded = Base64.getDecoder().decode(normalizedPem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new BusinessException(500, "商户私钥格式不正确");
        }
    }

    private PublicKey parsePublicKey(String pemContent) {
        try {
            String normalizedPem = normalizePem(pemContent)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "");
            byte[] encoded = Base64.getDecoder().decode(normalizedPem);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception e) {
            throw new BusinessException(500, "微信支付平台公钥格式不正确");
        }
    }

    private String normalizePem(String pemContent) {
        return pemContent
                .replace("\r", "")
                .replace("\n", "")
                .trim();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
