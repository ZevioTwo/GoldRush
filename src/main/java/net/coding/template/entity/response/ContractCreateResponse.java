package net.coding.template.entity.response;

import lombok.Data;
import net.coding.template.entity.dto.ContractDetailDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ContractCreateResponse {
    private String contractId;
    private String contractNo;
    private String status;
    private String title;
    private String gameType;
    private BigDecimal depositAmount;
    private BigDecimal serviceFeeAmount;
    private ContractDetailDTO.UserInfo receiverInfo;
    private LocalDateTime createTime;
    private PaymentInfo paymentInfo; // 支付信息

    @Data
    public static class PaymentInfo {
        private String prepayId;
        private String nonceStr;
        private String timeStamp;
        private String signType;
        private String paySign;
    }
}
