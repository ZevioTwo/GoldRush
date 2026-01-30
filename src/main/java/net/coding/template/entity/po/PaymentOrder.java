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

    // 在PaymentOrder实体类中添加以下字段
    @TableField("business_data")
    private String businessData; // 业务扩展数据，JSON格式存储额外信息

    @TableField("callback_status")
    private String callbackStatus = "PENDING"; // 回调状态：PENDING, SUCCESS, FAILED

    @TableField("callback_count")
    private Integer callbackCount = 0; // 回调次数

    @TableField("last_callback_time")
    private LocalDateTime lastCallbackTime; // 最后回调时间

    @TableField("notify_url")
    private String notifyUrl; // 异步通知URL
}
