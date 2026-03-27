package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserGameAccountCreateRequest {
    @NotBlank(message = "游戏名称不能为空")
    private String gameName;

    @NotBlank(message = "游戏账号不能为空")
    private String gameUid;

    @NotBlank(message = "游戏昵称不能为空")
    private String gameNickname;

    private String remark;
}
