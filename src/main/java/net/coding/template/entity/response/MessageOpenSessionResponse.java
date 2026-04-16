package net.coding.template.entity.response;

import lombok.Data;

@Data
public class MessageOpenSessionResponse {
    private Long sessionId;
    private String sessionType;
    private String peerName;
}
