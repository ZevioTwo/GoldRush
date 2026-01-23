package net.coding.template.entity.enums;

import lombok.Getter;

@Getter
public enum ContractPhase {
    PREPARE("PREPARE", "准备中"),
    IN_GAME("IN_GAME", "游戏中"),
    SETTLEMENT("SETTLEMENT", "结算中");

    private final String code;
    private final String description;

    ContractPhase(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
