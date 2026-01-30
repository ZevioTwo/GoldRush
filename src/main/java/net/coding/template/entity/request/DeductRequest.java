package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DeductRequest {
    @NotBlank(message = "契约ID不能为空")
    private String contractId;

    @NotNull(message = "违约者用户ID不能为空")
    private Long violatorUserId;

    @NotNull(message = "受害者用户ID不能为空")
    private Long victimUserId;

    @NotNull(message = "扣除金额不能为空")
    @DecimalMin(value = "0.01", message = "扣除金额不能小于0.01元")
    private BigDecimal deductAmount;

    @NotBlank(message = "违约原因不能为空")
    private String reason;

    private List<String> evidence; // 证据列表（图片/视频URL）

    private String remark; // 备注
}
