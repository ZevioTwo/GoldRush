package net.coding.template.entity.dto;

import lombok.Data;

@Data
public class CreditRankingItemDTO {
    private Long userId;
    private Integer rank;
    private String name;
    private String avatarUrl;
    private Integer score;
    private String status;
    private Integer totalContracts;
    private Integer completedContracts;
    private Integer violationCount;
}
