package net.coding.template.entity.request;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String wechatId;
    private String phone;
}
