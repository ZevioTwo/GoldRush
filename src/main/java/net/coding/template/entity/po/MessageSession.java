package net.coding.template.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message_sessions")
public class MessageSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("session_key")
    private String sessionKey;

    @TableField("session_type")
    private String sessionType;

    @TableField("peer_user_id")
    private Long peerUserId;

    @TableField("peer_name")
    private String peerName;

    @TableField("peer_avatar")
    private String peerAvatar;

    @TableField("peer_tag")
    private String peerTag;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_id")
    private String bizId;

    @TableField("last_message")
    private String lastMessage;

    @TableField("last_time")
    private LocalDateTime lastTime;

    @TableField("unread_count")
    private Integer unreadCount;

    @TableField("highlight")
    private Boolean highlight;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
