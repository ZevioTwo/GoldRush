package net.coding.template.entity.enums;

import lombok.Getter;

@Getter
public enum ConfigGroup {
    COMMON("COMMON", "通用配置"),
    PAYMENT("PAYMENT", "支付配置"),
    CREDIT("CREDIT", "信用配置"),
    CONTRACT("CONTRACT", "契约配置"),
    VIP("VIP", "VIP配置"),
    SECURITY("SECURITY", "安全配置"),
    NOTIFICATION("NOTIFICATION", "通知配置"),
    OPERATION("OPERATION", "运营配置"),
    THIRD_PARTY("THIRD_PARTY", "第三方配置"),
    CUSTOM("CUSTOM", "自定义配置");

    private final String code;
    private final String description;

    ConfigGroup(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ConfigGroup fromCode(String code) {
        for (ConfigGroup group : values()) {
            if (group.getCode().equals(code)) {
                return group;
            }
        }
        return CUSTOM;
    }
}
