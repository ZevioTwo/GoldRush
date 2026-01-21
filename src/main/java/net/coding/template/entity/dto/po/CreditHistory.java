package net.coding.template.entity.dto.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("credit_history")
public class CreditHistory {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("change_type")
    private String changeType;

    @TableField("change_amount")
    private Integer changeAmount;

    @TableField("before_score")
    private Integer beforeScore;

    @TableField("after_score")
    private Integer afterScore;

    @TableField("related_id")
    private String relatedId;

    @TableField("related_type")
    private String relatedType;

    @TableField("description")
    private String description;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
