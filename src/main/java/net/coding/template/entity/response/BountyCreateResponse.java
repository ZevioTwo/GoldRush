package net.coding.template.entity.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BountyCreateResponse {
    private Long bountyId;
    private String bountyNo;
    private BigDecimal rewardMojin;
    private BigDecimal totalRewardMojin;
    private Integer recruitTargetCount;
    private BigDecimal remainingMojinBalance;
}
