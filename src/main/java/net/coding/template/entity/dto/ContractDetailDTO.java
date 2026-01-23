package net.coding.template.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ContractDetailDTO {
    // 契约基本信息
    private String contractId;
    private String contractNo;
    private String status;
    private String phase;

    // 双方信息
    private UserInfo initiator;
    private UserInfo receiver;

    // 契约条款
    private String gameType;
    private String gameRegion;
    private BigDecimal depositAmount;
    private BigDecimal serviceFeeAmount;
    private String guaranteeItem;
    private String successCondition;
    private String failureCondition;

    // 时间信息
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime completeTime;

    // 确认状态
    private ConfirmStatus confirmStatus;
    private Boolean canConfirm; // 当前用户是否可以确认

    // 操作权限
    private Boolean canCancel;   // 是否可以取消
    private Boolean canStart;    // 是否可以开始
    private Boolean canComplete; // 是否可以完成
    private Boolean canDispute;  // 是否可以申请仲裁

    @Data
    public static class UserInfo {
        private Long userId;
        private String nickname;
        private String avatarUrl;
        private String gameId;
        private Integer creditScore;
        private Boolean isVip;
        private Integer completedContracts;
        private Double successRate;
    }

    @Data
    public static class ConfirmStatus {
        private Boolean initiatorConfirmed;
        private Boolean receiverConfirmed;
        private LocalDateTime initiatorConfirmTime;
        private LocalDateTime receiverConfirmTime;
    }
}
