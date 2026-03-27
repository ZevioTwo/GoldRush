package net.coding.template.entity.request;

import lombok.Data;
import javax.validation.constraints.*;

import java.math.BigDecimal;

@Data
public class ContractCreateRequest {
    @NotNull(message = "押金金额不能为空")
    @DecimalMin(value = "0.00", message = "押金金额不能低于0元")
    @DecimalMax(value = "200.00", message = "押金金额不能超过200元")
    private BigDecimal depositAmount;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过100字")
    private String title;

    @NotBlank(message = "游戏类型不能为空")
    @Size(max = 50, message = "游戏类型不能超过50字")
    private String gameType;

    @NotBlank(message = "契约达成条件不能为空")
    @Size(max = 500, message = "成功条件不能超过500字")
    private String successCondition;

    @Size(max = 500, message = "失败条件不能超过500字")
    private String failureCondition;

    @DecimalMin(value = "0", message = "最低信誉分不能为负")
    @DecimalMax(value = "10000", message = "最低信誉分过高")
    private Integer minCredit;

    @Size(max = 500, message = "要求不能超过500字")
    private String requirements;

    @Size(max = 2000, message = "描述不能超过2000字")
    private String description;

    @Size(max = 500, message = "封面URL过长")
    private String coverUrl;
}
