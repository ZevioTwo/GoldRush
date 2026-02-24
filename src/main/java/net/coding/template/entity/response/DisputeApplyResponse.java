package net.coding.template.entity.response;

import lombok.Data;

@Data
public class DisputeApplyResponse {
    private String disputeNo;
    private String contractId;
    private String status;
    private Boolean urgent;
    private String message;
}
