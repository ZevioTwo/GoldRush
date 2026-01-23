package net.coding.template.entity.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContractConfirmResponse {
    private String contractId;
    private String status; // WAITING_OTHER, COMPLETED
    private String message;
    private LocalDateTime confirmTime;
    private LocalDateTime completeTime;
}
