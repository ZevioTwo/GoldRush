package net.coding.template.entity.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class LoginRequest {
    @NotBlank(message = "code不能为空")
    private String code;          // 微信临时登录凭证

    private String rawData;       // 原始用户数据
    private String signature;     // 数据签名
    private String encryptedData; // 加密数据
    private String iv;            // 加密算法的初始向量
}
