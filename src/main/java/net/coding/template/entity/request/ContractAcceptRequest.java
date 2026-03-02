package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ContractAcceptRequest {
    @NotBlank(message = "契约ID不能为空")
    private String contractId;
}
