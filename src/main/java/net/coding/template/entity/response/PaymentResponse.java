package net.coding.template.entity.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class PaymentResponse {
    private Long orderId;
    private String orderNo;
    private BigDecimal amount;
    private Map<String, String> payParams; // 支付参数（用于前端调起支付）
    private LocalDateTime expireTime;
    private String qrCodeUrl; // 二维码URL（如果需要）
}