package net.coding.template.entity.request;

import lombok.Data;

@Data
public class ContractListRequest {
    private Integer page = 1;
    private Integer size = 20;
    private String status; // 状态筛选
    private String gameType; // 游戏类型筛选
    private String role = "ALL"; // 角色：ALL, INITIATOR, RECEIVER
}
