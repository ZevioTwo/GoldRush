package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class DisputeJudgeRequest {
    @NotBlank(message = "仲裁编号不能为空")
    private String disputeNo;

    @NotBlank(message = "裁决结果不能为空")
    private String result; // APPLICANT_WIN/RESPONDENT_WIN/DISMISSED

    @NotBlank(message = "裁决理由不能为空")
    @Size(max = 500, message = "裁决理由不能超过500字")
    private String resultReason;

    @NotNull(message = "是否违约不能为空")
    private Boolean violation;

    @DecimalMin(value = "0.00", message = "扣款金额不能为负")
    private BigDecimal deductAmount;
}
