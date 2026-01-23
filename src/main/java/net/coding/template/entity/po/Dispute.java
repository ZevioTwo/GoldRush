package net.coding.template.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("disputes")
public class Dispute {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("dispute_no")
    private String disputeNo;

    @TableField("contract_id")
    private String contractId;

    @TableField("applicant_id")
    private Long applicantId;

    @TableField("respondent_id")
    private Long respondentId;

    @TableField("applicant_role")
    private String applicantRole;

    @TableField("dispute_type")
    private String disputeType;

    @TableField("description")
    private String description;

    @TableField("evidence_urls")
    private String evidenceUrls;

    @TableField("game_screenshot_urls")
    private String gameScreenshotUrls;

    @TableField("video_links")
    private String videoLinks;

    @TableField("status")
    private String status;

    @TableField("result")
    private String result;

    @TableField("result_reason")
    private String resultReason;

    @TableField("is_urgent")
    private Boolean isUrgent = false;

    @TableField("urgent_fee_paid")
    private Boolean urgentFeePaid = false;

    @TableField("urgent_fee_order_id")
    private Long urgentFeeOrderId;

    @TableField("handler_id")
    private Long handlerId;

    @TableField("handle_time")
    private LocalDateTime handleTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
