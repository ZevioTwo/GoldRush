package net.coding.template.entity.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserGameAccountResponse {
    private Long id;
    private String gameType;
    private String gameRegion;
    private String gameId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
