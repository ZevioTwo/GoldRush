package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class UserGameAccountCreateRequest {
    @NotBlank(message = "游戏类型不能为空")
    private String gameType;

    @NotBlank(message = "游戏大区不能为空")
    private String gameRegion;

    @NotBlank(message = "游戏ID不能为空")
    private String gameId;

    @Size(max = 100, message = "备注不能超过100字")
    private String remark;
}
