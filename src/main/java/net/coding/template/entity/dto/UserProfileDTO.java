package net.coding.template.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserProfileDTO {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer creditScore;
    private Boolean isVip;
    private LocalDateTime vipExpireTime;
    private Integer totalContracts;
    private Integer completedContracts;
    private Double successRate;      // 成功率
    private Integer disputeCount;
    private Integer violationCount;
    private String userLevel;        // 用户等级
    private String wechatId;
    private String phone;
}
