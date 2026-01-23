package net.coding.template.entity.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreditScoreDTO {
    private Integer currentScore;      // 当前信用分
    private Integer maxScore = 100;    // 最高信用分
    private String level;              // 信用等级
    private List<CreditHistory> history; // 信用历史

    @Data
    public static class CreditHistory {
        private String type;           // 变动类型：COMPLETE, VIOLATION, DISPUTE
        private String description;    // 描述
        private Integer change;        // 变动值（+10, -50等）
        private String time;           // 变动时间
    }
}
