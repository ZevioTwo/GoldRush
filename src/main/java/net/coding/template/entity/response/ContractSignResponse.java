package net.coding.template.entity.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContractSignResponse {
    private String contractId;
    private String status; // ACTIVE
    private String message;
    private LocalDateTime signTime;
}