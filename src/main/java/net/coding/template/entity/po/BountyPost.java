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
@TableName("bounty_posts")
public class BountyPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("bounty_no")
    private String bountyNo;

    @TableField("creator_user_id")
    private Long creatorUserId;

    @TableField("creator_credit_score")
    private Integer creatorCreditScore;

    @TableField("target_role_name")
    private String targetRoleName;

    @TableField("title")
    private String title;

    @TableField("reward_mojin")
    private BigDecimal rewardMojin;

    @TableField("total_reward_mojin")
    private BigDecimal totalRewardMojin;

    @TableField("game_type")
    private String gameType;

    @TableField("description")
    private String description;

    @TableField("recruit_target_count")
    private Integer recruitTargetCount;

    @TableField("recruit_current_count")
    private Integer recruitCurrentCount;

    @TableField("status")
    private String status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
