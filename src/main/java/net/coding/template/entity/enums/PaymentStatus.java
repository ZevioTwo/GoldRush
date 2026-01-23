package net.coding.template.entity.enums;

import lombok.Getter;

@Getter
public enum PaymentStatus {
    PENDING("PENDING", "待支付"),
    SUCCESS("SUCCESS", "支付成功"),
    FAILED("FAILED", "支付失败"),
    CLOSED("CLOSED", "已关闭"),
    REFUNDING("REFUNDING", "退款中"),
    REFUNDED("REFUNDED", "已退款"),
    REFUND_FAILED("REFUND_FAILED", "退款失败");

    private final String code;
    private final String description;

    PaymentStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PaymentStatus fromCode(String code) {
        for (PaymentStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return PENDING;
    }

    /**
     * 判断是否是最终状态（不可再变更）
     */
    public boolean isFinalStatus() {
        return this == SUCCESS || this == FAILED || this == CLOSED || this == REFUNDED;
    }

    /**
     * 判断是否是成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 判断是否可退款
     */
    public boolean canRefund() {
        return this == SUCCESS;
    }
}
