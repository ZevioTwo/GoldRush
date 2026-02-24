package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ContractAcceptRequest {
    @NotBlank(message = "契约ID不能为空")
    private String contractId;

    @NotNull(message = "接单账号ID不能为空")
    private Long receiverAccountId;
}
