package net.coding.template.entity.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreditRankingDTO {
    private String updateTime;
    private List<CreditRankingItemDTO> redList = new ArrayList<>();
    private List<CreditRankingItemDTO> blackList = new ArrayList<>();
}
