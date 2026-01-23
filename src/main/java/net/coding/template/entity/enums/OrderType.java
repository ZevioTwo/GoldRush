package net.coding.template.entity.enums;

import lombok.Getter;

@Getter
public enum OrderType {
    SERVICE_FEE("SERVICE_FEE", "平台服务费"),
    DEPOSIT_FREEZE("DEPOSIT_FREEZE", "押金冻结"),
    DEPOSIT_DEDUCT("DEPOSIT_DEDUCT", "押金扣除"),
    DEPOSIT_UNFREEZE("DEPOSIT_UNFREEZE", "押金解冻"),
    VIP_PAYMENT("VIP_PAYMENT", "VIP会员支付"),
    ARBITRATION_FEE("ARBITRATION_FEE", "仲裁加急费"),
    COMPENSATION("COMPENSATION", "违约赔偿金"),
    REFUND("REFUND", "退款订单");

    private final String code;
    private final String description;

    OrderType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static OrderType fromCode(String code) {
        for (OrderType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return SERVICE_FEE;
    }

    /**
     * 判断是否是支付类型（需要用户实际支付）
     */
    public boolean isPaymentType() {
        return this == SERVICE_FEE || this == DEPOSIT_FREEZE ||
                this == VIP_PAYMENT || this == ARBITRATION_FEE;
    }

    /**
     * 判断是否是押金相关类型
     */
    public boolean isDepositType() {
        return this == DEPOSIT_FREEZE || this == DEPOSIT_DEDUCT || this == DEPOSIT_UNFREEZE;
    }

    /**
     * 判断是否是冻结类型
     */
    public boolean isFreezeType() {
        return this == DEPOSIT_FREEZE;
    }

    /**
     * 判断是否是解冻类型
     */
    public boolean isUnfreezeType() {
        return this == DEPOSIT_UNFREEZE;
    }
}
