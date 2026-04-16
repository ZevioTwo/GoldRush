package net.coding.template.entity.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditExchangeResponse {
    private BigDecimal exchangedMojin;
    private Integer addedCreditScore;
    private BigDecimal beforeMojinBalance;
    private BigDecimal afterMojinBalance;
    private Integer beforeCreditScore;
    private Integer afterCreditScore;
}
