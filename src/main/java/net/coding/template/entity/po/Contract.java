package net.coding.template.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("contracts")
public class Contract {
    @TableId
    private String id;

    @TableField("contract_no")
    private String contractNo;

    @TableField("initiator_id")
    private Long initiatorId;

    @TableField("receiver_id")
    private Long receiverId;

    @TableField("deposit_amount")
    private BigDecimal depositAmount;

    @TableField("service_fee_amount")
    private BigDecimal serviceFeeAmount;

    @TableField("penalty_amount")
    private BigDecimal penaltyAmount;

    @TableField("title")
    private String title;

    @TableField("guarantee_item")
    private String guaranteeItem;

    @TableField("success_condition")
    private String successCondition;

    @TableField("failure_condition")
    private String failureCondition;

    @TableField("status")
    private String status;

    @TableField("phase")
    private String phase;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("complete_time")
    private LocalDateTime completeTime;

    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    @TableField("violate_time")
    private LocalDateTime violateTime;

    @TableField("payment_status")
    private String paymentStatus;

    @TableField("freeze_status")
    private String freezeStatus;

    @TableField("refund_status")
    private String refundStatus;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
