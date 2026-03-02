package net.coding.template.entity.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContractListResponse {
    private Integer total;
    private Integer page;
    private Integer size;
    private Integer totalPages;
    private List<ContractItem> contracts;

    @Data
    public static class ContractItem {
        private String contractId;
        private String contractNo;
        private String title;
        private String status;
        private BigDecimal depositAmount;
        private String guaranteeItem;
        private Integer opponentCreditScore;
        private LocalDateTime createTime;
        private LocalDateTime completeTime;
    }
}
