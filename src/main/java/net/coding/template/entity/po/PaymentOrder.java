package net.coding.template.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_orders")
public class PaymentOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("contract_id")
    private String contractId;

    @TableField("user_id")
    private Long userId;

    @TableField("order_type")
    private String orderType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("fee_rate")
    private BigDecimal feeRate;

    @TableField("fee_amount")
    private BigDecimal feeAmount;

    @TableField("payment_method")
    private String paymentMethod;

    @TableField("wx_prepay_id")
    private String wxPrepayId;

    @TableField("wx_transaction_id")
    private String wxTransactionId;

    @TableField("wx_out_trade_no")
    private String wxOutTradeNo;

    @TableField("freeze_contract_id")
    private String freezeContractId;

    @TableField("freeze_transaction_id")
    private String freezeTransactionId;

    @TableField("unfreeze_transaction_id")
    private String unfreezeTransactionId;

    @TableField("payment_status")
    private String paymentStatus;

    @TableField("refund_status")
    private String refundStatus;

    @TableField("is_settled")
    private Boolean isSettled = false;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("freeze_time")
    private LocalDateTime freezeTime;

    @TableField("unfreeze_time")
    private LocalDateTime unfreezeTime;

    @TableField("refund_time")
    private LocalDateTime refundTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
