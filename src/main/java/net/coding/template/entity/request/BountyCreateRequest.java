package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class BountyCreateRequest {

    @NotBlank(message = "被悬赏角色名称不能为空")
    @Size(max = 100, message = "被悬赏角色名称不能超过100字")
    private String targetRoleName;

    @NotBlank(message = "悬赏标题不能为空")
    @Size(max = 100, message = "悬赏标题不能超过100字")
    private String title;

    @NotNull(message = "悬赏摸金币不能为空")
    @DecimalMin(value = "0.01", message = "每位摸金校尉奖励至少为0.01摸金币")
    private BigDecimal rewardMojin;

    @NotBlank(message = "游戏类型不能为空")
    @Size(max = 50, message = "游戏类型不能超过50字")
    private String gameType;

    @NotBlank(message = "悬赏描述不能为空")
    @Size(max = 1000, message = "悬赏描述不能超过1000字")
    private String description;

    @NotNull(message = "招募数量不能为空")
    @Min(value = 1, message = "招募数量至少为1")
    private Integer recruitTargetCount;
}
