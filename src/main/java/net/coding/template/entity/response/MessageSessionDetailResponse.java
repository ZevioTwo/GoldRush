package net.coding.template.entity.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MessageSessionDetailResponse {
    private Long sessionId;
    private String sessionType;
    private String peerName;
    private String peerAvatar;
    private String peerTag;
    private String bizType;
    private String bizId;
    private Boolean canReply;
    private List<MessageItemDTO> items;

    @Data
    public static class MessageItemDTO {
        private Long id;
        private Long senderId;
        private String content;
        private String msgType;
        private LocalDateTime createTime;
        private Boolean self;
    }
}
