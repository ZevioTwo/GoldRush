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
    private String title;

    // 双方信息
    private UserInfo initiator;
    private UserInfo receiver;

    // 契约条款
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
    private LocalDateTime updateTime;
    private LocalDateTime acceptExpireTime; // 接单超时截止时间

    // 确认状态
    private ConfirmStatus confirmStatus;
    private Boolean canConfirm; // 当前用户是否可以确认

    // 操作权限
    private Boolean canCancel;   // 是否可以取消
    private Boolean canStart;    // 是否可以开始
    private Boolean canComplete; // 是否可以完成
    private Boolean canDispute;  // 是否可以申请仲裁

    // 新增：签订/完成按钮控制
    private Boolean canSign;     // 甲方是否可签订
    private Boolean canFinish;   // 乙方是否可完成
    private String role;         // INITIATOR / RECEIVER
    private String signerId;     // 最近签订方
    private Boolean canFinishBySigner; // 是否为被签订方
    private Boolean depositRequired; // 是否需要保证金

    @Data
    public static class UserInfo {
        private Long userId;
        private String nickname;
        private String avatarUrl;
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
