package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class PaymentRequest {
    private String contractId;

    @NotBlank(message = "订单类型不能为空")
    private String orderType; // SERVICE_FEE, DEPOSIT_FREEZE, VIP_PAYMENT, ARBITRATION_FEE, CREDIT_RECHARGE(摸金币充值)

    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额不能小于0.01元")
    private BigDecimal amount;

    private String payMethod; // WECHAT/ALIPAY

    private String returnUrl; // 支付完成后的返回URL
    private String notifyUrl; // 异步通知URL（可选，默认用系统配置）
    private Map<String, Object> extraData; // 扩展数据
}
