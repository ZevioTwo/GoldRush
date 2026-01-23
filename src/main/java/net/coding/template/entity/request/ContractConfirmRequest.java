package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ContractConfirmRequest {
    @NotBlank(message = "契约ID不能为空")
    private String contractId;

    private String endTime; // 游戏结束时间（可选）
}
