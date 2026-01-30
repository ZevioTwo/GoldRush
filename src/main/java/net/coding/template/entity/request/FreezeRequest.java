package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class FreezeRequest {
    @NotBlank(message = "契约ID不能为空")
    private String contractId;

    @NotNull(message = "冻结金额不能为空")
    @DecimalMin(value = "10.00", message = "冻结金额不能低于10元")
    private BigDecimal freezeAmount;

    private String remark; // 备注
}
