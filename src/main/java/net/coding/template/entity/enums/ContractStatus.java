package net.coding.template.entity.enums;

import lombok.Getter;

@Getter
public enum ContractStatus {
    PENDING("PENDING", "待支付"),
    PAID("PAID", "已支付"),
    ACTIVE("ACTIVE", "进行中"),
    IN_GAME("IN_GAME", "游戏中"),
    COMPLETED("COMPLETED", "已完成"),
    DISPUTE("DISPUTE", "争议中"),
    CANCELLED("CANCELLED", "已取消"),
    VIOLATED("VIOLATED", "已违约"),
    ARCHIVED("ARCHIVED", "已归档");

    private final String code;
    private final String description;

    ContractStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ContractStatus fromCode(String code) {
        for (ContractStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
