package net.coding.template.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("openid")
    private String openid;

    @TableField("unionid")
    private String unionid;

    @TableField("nickname")
    private String nickname;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("phone")
    private String phone;

    @TableField("game_id")
    private String gameId;

    @TableField("game_region")
    private String gameRegion;

    @TableField("wechat_id")
    private String wechatId;

    @TableField("credit_score")
    private Integer creditScore = 100;

    @TableField("total_contracts")
    private Integer totalContracts = 0;

    @TableField("completed_contracts")
    private Integer completedContracts = 0;

    @TableField("dispute_count")
    private Integer disputeCount = 0;

    @TableField("violation_count")
    private Integer violationCount = 0;

    @TableField("is_vip")
    private Boolean isVip = false;

    @TableField("vip_type")
    private String vipType;

    @TableField("vip_start_time")
    private LocalDateTime vipStartTime;

    @TableField("vip_expire_time")
    private LocalDateTime vipExpireTime;

    @TableField("vip_contract_count")
    private Integer vipContractCount = 0;

    @TableField("status")
    private String status = "ACTIVE";

    @TableField("blacklist_reason")
    private String blacklistReason;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField("last_login_ip")
    private String lastLoginIp;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
