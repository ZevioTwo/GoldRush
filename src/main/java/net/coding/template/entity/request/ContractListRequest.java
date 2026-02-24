package net.coding.template.entity.request;

import lombok.Data;

@Data
public class ContractListRequest {
    private Integer page = 1;
    private Integer size = 20;
    private String status; // 状态筛选
    private String gameType; // 游戏类型筛选
    private String role = "ALL"; // 角色：ALL, INITIATOR, RECEIVER
    private String scope = "ALL"; // 列表范围：ALL, INITIATED, RECEIVED
    private String keyword; // 标题关键字（模糊）
    private String contractNo; // 契约号关键字（模糊）
    private String initiatorGameId; // 发起人游戏ID（精确）
}
