package net.coding.template.entity.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeductResponse {
    private Boolean success;
    private String contractId;
    private Long deductOrderId;
    private Long compensationOrderId;
    private BigDecimal deductAmount;
    private BigDecimal compensationAmount;
    private String message;
    private String errorCode;
}
