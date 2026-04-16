package net.coding.template.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mojin_ledger")
public class MojinLedger {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("change_amount")
    private BigDecimal changeAmount;

    @TableField("before_balance")
    private BigDecimal beforeBalance;

    @TableField("after_balance")
    private BigDecimal afterBalance;

    @TableField("change_type")
    private String changeType;

    @TableField("related_id")
    private String relatedId;

    @TableField("related_type")
    private String relatedType;

    @TableField("description")
    private String description;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
