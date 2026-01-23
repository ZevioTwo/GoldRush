package net.coding.template.entity.enums;

import lombok.Getter;

@Getter
public enum ConfigType {
    STRING("STRING", "字符串"),
    NUMBER("NUMBER", "数字"),
    BOOLEAN("BOOLEAN", "布尔值"),
    JSON("JSON", "JSON对象"),
    ARRAY("ARRAY", "数组"),
    OBJECT("OBJECT", "复杂对象");

    private final String code;
    private final String description;

    ConfigType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ConfigType fromCode(String code) {
        for (ConfigType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return STRING;
    }
}
