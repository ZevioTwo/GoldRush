package net.coding.template.entity.dto;

import lombok.Data;

@Data
public class UserStatsDTO {
    private Integer activeContracts;
    private Integer completedContracts;
    private Double successRate;
}
