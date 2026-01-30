package net.coding.template.entity.response;

import lombok.Data;

import java.util.List;

@Data
public class UnfreezeResponse {
    private Boolean success;
    private String contractId;
    private List<UnfreezeResult> results;
    private String message;
    private String errorCode;

    @Data
    public static class UnfreezeResult {
        private Long userId;
        private Long freezeOrderId;
        private Long unfreezeOrderId;
        private java.math.BigDecimal freezeAmount;
        private Boolean success;
        private String message;
    }
}
