package net.coding.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.coding.template.entity.po.MessageSession;
import net.coding.template.entity.response.MessageSessionListResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MessageSessionMapper extends BaseMapper<MessageSession> {

    @Select("SELECT " +
            "id AS sessionId, " +
            "session_type AS sessionType, " +
            "peer_name AS peerName, " +
            "peer_avatar AS peerAvatar, " +
            "peer_tag AS peerTag, " +
            "biz_type AS bizType, " +
            "biz_id AS bizId, " +
            "last_message AS lastMessage, " +
            "last_time AS lastTime, " +
            "unread_count AS unreadCount, " +
            "highlight AS highlight " +
            "FROM message_sessions " +
            "WHERE user_id = #{userId} " +
            "ORDER BY CASE WHEN unread_count > 0 THEN 0 ELSE 1 END, highlight DESC, COALESCE(last_time, create_time) DESC, id DESC")
    List<MessageSessionListResponse.MessageSessionItem> selectUserSessions(@Param("userId") Long userId);

    @Select("SELECT * FROM message_sessions WHERE id = #{sessionId} AND user_id = #{userId} LIMIT 1")
    MessageSession selectOwnedSession(@Param("sessionId") Long sessionId,
                                      @Param("userId") Long userId);

    @Select("SELECT * FROM message_sessions WHERE user_id = #{userId} AND session_key = #{sessionKey} ORDER BY id ASC LIMIT 1")
    MessageSession selectByUserAndSessionKey(@Param("userId") Long userId,
                                             @Param("sessionKey") String sessionKey);

    @Update("UPDATE message_sessions SET last_message = #{lastMessage}, last_time = #{lastTime}, update_time = NOW() WHERE id = #{sessionId}")
    int updateSessionSnapshot(@Param("sessionId") Long sessionId,
                              @Param("lastMessage") String lastMessage,
                              @Param("lastTime") LocalDateTime lastTime);

    @Update("UPDATE message_sessions SET unread_count = unread_count + 1, update_time = NOW() WHERE id = #{sessionId}")
    int incrementUnread(@Param("sessionId") Long sessionId);

    @Update("UPDATE message_sessions SET unread_count = 0, update_time = NOW() WHERE id = #{sessionId} AND user_id = #{userId}")
    int clearUnread(@Param("sessionId") Long sessionId,
                    @Param("userId") Long userId);
}
