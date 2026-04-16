package net.coding.template.entity.request;

import lombok.Data;

@Data
public class BountyListRequest {
    private Integer page = 1;
    private Integer size = 20;
    private String keyword;
    private String gameType;
}
