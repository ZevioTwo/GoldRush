package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class UserProfileUpdateRequest {
    @NotBlank(message = "游戏ID不能为空")
    @Size(max = 50, message = "游戏ID不能超过50字")
    private String gameId;

    @NotBlank(message = "游戏大区不能为空")
    @Size(max = 50, message = "游戏大区不能超过50字")
    private String gameRegion;

    @Size(max = 50, message = "微信号不能超过50字")
    private String wechatId;

    @Size(max = 20, message = "手机号不能超过20字")
    private String phone;
}
