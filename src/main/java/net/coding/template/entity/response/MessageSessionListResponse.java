package net.coding.template.entity.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MessageSessionListResponse {
    private Integer unreadTotal;
    private List<MessageSessionItem> sessions;

    @Data
    public static class MessageSessionItem {
        private Long sessionId;
        private String sessionType;
        private String peerName;
        private String peerAvatar;
        private String peerTag;
        private String bizType;
        private String bizId;
        private String lastMessage;
        private LocalDateTime lastTime;
        private Integer unreadCount;
        private Boolean highlight;
        private Boolean canReply;
    }
}
