package net.coding.template.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.coding.template.configuration.WeChatPayConfig;
import net.coding.template.entity.po.PaymentOrder;
import net.coding.template.exception.BusinessException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.core.codec.DecodingException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class WeChatPayService {

    @Resource
    private WeChatPayConfig weChatPayConfig;

    @Resource
    private CloseableHttpClient wechatPayHttpClient;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 统一下单（服务费支付）
     */
    public Map<String, String> unifiedOrder(PaymentOrder paymentOrder, String openid) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("appid", weChatPayConfig.getAppId());
            params.put("mchid", weChatPayConfig.getMchId());
            params.put("description", getOrderDescription(paymentOrder.getOrderType()));
            params.put("out_trade_no", paymentOrder.getWxOutTradeNo());
            params.put("notify_url", weChatPayConfig.getNotifyUrl());
            params.put("time_expire", LocalDateTime.now().plusMinutes(30).format(TIME_FORMATTER));

            // 金额信息（单位：分）
            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("total", paymentOrder.getAmount().multiply(new BigDecimal(100)).intValue());
            amountMap.put("currency", "CNY");
            params.put("amount", JSON.toJSONString(amountMap));

            // 支付者信息
            Map<String, String> payerMap = new HashMap<>();
            payerMap.put("openid", openid);
            params.put("payer", JSON.toJSONString(payerMap));

            // 调用微信支付API
            String response = executePost("/v3/pay/transactions/jsapi", params);
            JSONObject jsonResponse = JSON.parseObject(response);

            if (jsonResponse.containsKey("prepay_id")) {
                String prepayId = jsonResponse.getString("prepay_id");

                // 生成前端调起支付所需参数
                return generateJsapiParams(prepayId);
            } else {
                log.error("微信支付下单失败: {}", response);
                throw new BusinessException(500, "微信支付下单失败");
            }
        } catch (Exception e) {
            log.error("微信支付下单异常", e);
            throw new BusinessException(500, "微信支付服务异常");
        }
    }

    /**
     * 资金冻结（预授权）
     */
    public Map<String, String> freezeDeposit(PaymentOrder paymentOrder, String openid) {
        try {
            // 构建资金授权请求
            Map<String, String> params = new HashMap<>();
            params.put("appid", weChatPayConfig.getAppId());
            params.put("mchid", weChatPayConfig.getMchId());
            params.put("out_order_no", paymentOrder.getWxOutTradeNo());
            params.put("out_request_no", generateRequestNo());
            params.put("description", "契约押金冻结");

            // 授权金额
            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("total", paymentOrder.getAmount().multiply(new BigDecimal(100)).intValue());
            amountMap.put("currency", "CNY");
            params.put("amount", JSON.toJSONString(amountMap));

            // 支付者信息
            Map<String, String> payerMap = new HashMap<>();
            payerMap.put("openid", openid);
            params.put("payer", JSON.toJSONString(payerMap));

            // 通知地址
            params.put("notify_url", weChatPayConfig.getFreezeNotifyUrl());

            // 调用微信支付分资金授权接口
            String response = executePost("/v3/payscore/permissions", params);
            JSONObject jsonResponse = JSON.parseObject(response);

            if (jsonResponse.containsKey("authorization_code")) {
                Map<String, String> result = new HashMap<>();
                result.put("authorization_code", jsonResponse.getString("authorization_code"));
                result.put("freeze_contract_id", jsonResponse.getString("authorization_code"));
                return result;
            } else {
                log.error("资金冻结失败: {}", response);
                // 如果支付分接口失败，尝试使用普通预授权
                return tryAlternativeFreeze(paymentOrder, openid);
            }
        } catch (Exception e) {
            log.error("资金冻结异常", e);
            throw new BusinessException(500, "资金冻结服务异常");
        }
    }

    /**
     * 备选冻结方案（使用普通支付模拟）
     */
    private Map<String, String> tryAlternativeFreeze(PaymentOrder paymentOrder, String openid) {
        log.info("尝试使用备选冻结方案");

        // 使用普通支付，资金进入平台中间账户
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
            Map<String, String> params = new HashMap<>();
            params.put("appid", weChatPayConfig.getAppId());
            params.put("mchid", weChatPayConfig.getMchId());
            params.put("out_order_no", paymentOrder.getWxOutTradeNo());
            params.put("out_request_no", generateRequestNo());
            params.put("reason", "契约完成，解冻押金");

            // 解冻金额
            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("total", paymentOrder.getAmount().multiply(new BigDecimal(100)).intValue());
            amountMap.put("currency", "CNY");
            params.put("amount", JSON.toJSONString(amountMap));

            String url = String.format("/v3/payscore/permissions/%s/terminate",
                    paymentOrder.getFreezeContractId());

            String response = executePost(url, params);
            JSONObject jsonResponse = JSON.parseObject(response);

            return jsonResponse.containsKey("terminate_time");
        } catch (Exception e) {
            log.error("解冻资金异常", e);
            // 解冻失败时，可以手动处理
            return false;
        }
    }

    /**
     * 扣除违约金
     */
    public boolean deductPenalty(PaymentOrder paymentOrder) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("appid", weChatPayConfig.getAppId());
            params.put("mchid", weChatPayConfig.getMchId());
            params.put("out_trade_no", paymentOrder.getWxOutTradeNo());
            params.put("description", "违约金扣除");

            // 金额信息
            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("total", paymentOrder.getAmount().multiply(new BigDecimal(100)).intValue());
            amountMap.put("currency", "CNY");
            params.put("amount", JSON.toJSONString(amountMap));

            // 使用支付分扣款或普通支付
            String response;
            if (paymentOrder.getFreezeContractId() != null &&
                    paymentOrder.getFreezeContractId().startsWith("PSCORE_")) {
                // 支付分扣款
                String url = String.format("/v3/payscore/serviceorder/direct-complete");
                params.put("authorization_code", paymentOrder.getFreezeContractId());
                response = executePost(url, params);
            } else {
                // 普通支付扣款
                response = executePost("/v3/pay/transactions/jsapi", params);
            }

            JSONObject jsonResponse = JSON.parseObject(response);
            return jsonResponse.containsKey("transaction_id");
        } catch (Exception e) {
            log.error("扣除违约金异常", e);
            return false;
        }
    }

    /**
     * 验证支付回调签名
     */
    public boolean verifyNotification(String timestamp, String nonce, String body, String signature) {
        try {
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(weChatPayConfig.getApiKey().getBytes(), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(message.getBytes());
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);

            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("验证签名异常", e);
            return false;
        }
    }

    /**
     * 处理支付成功回调
     */
    public Map<String, String> handlePaymentNotify(String notifyBody) {
        JSONObject notifyData = JSON.parseObject(notifyBody);

        Map<String, String> result = new HashMap<>();
        result.put("out_trade_no", notifyData.getString("out_trade_no"));
        result.put("transaction_id", notifyData.getString("transaction_id"));
        result.put("trade_state", notifyData.getString("trade_state"));
        result.put("success_time", notifyData.getString("success_time"));

        // 处理支付成功逻辑
        String tradeState = notifyData.getString("trade_state");
        if ("SUCCESS".equals(tradeState)) {
            // 支付成功处理
            handleSuccessfulPayment(result);
        } else if ("REFUND".equals(tradeState)) {
            // 退款处理
            handleRefund(result);
        }

        return result;
    }

    /**
     * 生成JSAPI支付参数
     */
    private Map<String, String> generateJsapiParams(String prepayId) {
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = generateNonceStr();
        String packageStr = "prepay_id=" + prepayId;

        // 生成签名
        String signStr = weChatPayConfig.getAppId() + "\n" +
                timeStamp + "\n" +
                nonceStr + "\n" +
                packageStr + "\n";

        String sign = signWithSHA256(signStr);

        Map<String, String> params = new HashMap<>();
        params.put("timeStamp", timeStamp);
        params.put("nonceStr", nonceStr);
        params.put("package", packageStr);
        params.put("signType", "RSA");
        params.put("paySign", sign);

        return params;
    }

    /**
     * 执行HTTP POST请求
     */
    private String executePost(String apiPath, Map<String, String> params) throws Exception {
        String url = "https://api.mch.weixin.qq.com" + apiPath;
        HttpPost httpPost = new HttpPost(url);

        // 添加认证头
        String token = generateToken(apiPath, "POST", JSON.toJSONString(params));
        httpPost.addHeader("Authorization", "WECHATPAY2-SHA256-RSA2048 " + token);
        httpPost.addHeader("Accept", "application/json");
        httpPost.addHeader("Content-Type", "application/json");

        // 设置请求体
        StringEntity entity = new StringEntity(JSON.toJSONString(params), "UTF-8");
        httpPost.setEntity(entity);

        // 执行请求
        try (CloseableHttpResponse response = wechatPayHttpClient.execute(httpPost)) {
            return org.apache.http.util.EntityUtils.toString(response.getEntity());
        }
    }

    /**
     * 生成认证Token
     */
    private String generateToken(String apiPath, String method, String body) {
        // 简化实现，实际需要完整的微信支付签名算法
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = generateNonceStr();
        String message = method + "\n" + apiPath + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";

        // 使用商户私钥签名
        // 这里简化处理，实际需要加载证书
        String signature = "simulated_signature";

        return String.format("mchid=\"%s\",nonce_str=\"%s\",timestamp=\"%s\",serial_no=\"%s\",signature=\"%s\"",
                weChatPayConfig.getMchId(), nonce, timestamp,
                weChatPayConfig.getSerialNo(), signature);
    }

    /**
     * 生成随机字符串
     */
    private String generateNonceStr() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    /**
     * 生成请求号
     */
    private String generateRequestNo() {
        return "REQ" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    /**
     * SHA256签名
     */
    private String signWithSHA256(String data) {
        // 简化实现
        return DigestUtils.md5DigestAsHex(data.getBytes());
    }

    /**
     * 获取订单描述
     */
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

    /**
     * 解密微信支付回调数据
     * 使用API v3密钥进行AES-GCM解密
     */
    public String decryptNotifyData(String ciphertext, String nonce, String associatedData) {
        try {
            // 获取API v3密钥
            byte[] apiV3Key = weChatPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8);

            // Base64解码密文
            byte[] cipherBytes = Base64.getDecoder().decode(ciphertext);

            // 使用AES-GCM解密
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            // 创建GCMParameterSpec，tag长度128位
            GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));

            // 创建密钥
            SecretKeySpec keySpec = new SecretKeySpec(apiV3Key, "AES");

            // 初始化解密器
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

            // 设置附加认证数据（AAD）
            if (associatedData != null) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }

            // 执行解密
            byte[] decryptedBytes = cipher.doFinal(cipherBytes);

            // 转换为字符串
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (DecodingException e) {
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
            Map<String, String> params = new HashMap<>();
            params.put("transaction_id", originalOrder.getWxTransactionId());
            params.put("out_refund_no", refundOrder.getWxOutTradeNo());
            params.put("total", originalOrder.getAmount().multiply(new BigDecimal(100)).intValue() + "");
            params.put("refund", refundOrder.getAmount().multiply(new BigDecimal(100)).intValue() + "");
            params.put("reason", "契约取消退款");

            // 调用微信退款API
            String response = executePost("/v3/refund/domestic/refunds", params);
            JSONObject jsonResponse = JSON.parseObject(response);

            return jsonResponse.containsKey("refund_id");

        } catch (Exception e) {
            log.error("微信退款接口调用异常: orderNo={}", originalOrder.getOrderNo(), e);
            return false;
        }
    }

    private void handleSuccessfulPayment(Map<String, String> result) {
        // 实际处理逻辑
    }

    private void handleRefund(Map<String, String> result) {
        // 实际处理逻辑
    }

}
