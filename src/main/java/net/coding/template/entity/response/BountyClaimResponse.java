package net.coding.template.entity.response;

import lombok.Data;

@Data
public class BountyClaimResponse {
    private Long bountyId;
    private Integer recruitCurrentCount;
    private Integer recruitTargetCount;
    private String status;
}
