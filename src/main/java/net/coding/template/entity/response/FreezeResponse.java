package net.coding.template.entity.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FreezeResponse {
    private Boolean success;
    private Long orderId;
    private String orderNo;
    private String contractId;
    private BigDecimal freezeAmount;
    private String authorizationCode;
    private String message;
    private String errorCode;
}
