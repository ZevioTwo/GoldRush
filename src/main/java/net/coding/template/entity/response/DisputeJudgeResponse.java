package net.coding.template.entity.response;

import lombok.Data;

@Data
public class DisputeJudgeResponse {
    private String disputeNo;
    private String contractId;
    private String status;
    private String result;
    private String message;
}
