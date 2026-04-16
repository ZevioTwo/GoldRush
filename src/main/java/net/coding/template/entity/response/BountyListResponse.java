package net.coding.template.entity.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BountyListResponse {
    private Integer total;
    private Integer page;
    private Integer size;
    private Integer totalPages;
    private List<BountyItem> items;

    @Data
    public static class BountyItem {
        private Long id;
        private String bountyNo;
        private String title;
        private String targetRoleName;
        private String gameType;
        private String description;
        private BigDecimal rewardMojin;
        private BigDecimal totalRewardMojin;
        private Integer recruitTargetCount;
        private Integer recruitCurrentCount;
        private Integer creatorCreditScore;
        private String status;
        private LocalDateTime createTime;
        private Boolean claimed;
        private Boolean canClaim;
    }
}
