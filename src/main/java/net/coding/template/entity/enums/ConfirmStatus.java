package net.coding.template.entity.enums;

import lombok.Getter;

@Getter
public enum ConfirmStatus {
    PENDING("PENDING", "待确认"),
    CONFIRMED("CONFIRMED", "已确认"),
    REJECTED("REJECTED", "已拒绝");

    private final String code;
    private final String description;

    ConfirmStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
