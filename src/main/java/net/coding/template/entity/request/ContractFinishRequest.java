package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ContractFinishRequest {
    @NotBlank(message = "契约ID不能为空")
    private String contractId;

    private String endTime; // 可选：结束时间
}