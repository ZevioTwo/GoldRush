package net.coding.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.coding.template.entity.po.MessageItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MessageItemMapper extends BaseMapper<MessageItem> {

    @Select("SELECT * FROM message_items WHERE session_id = #{sessionId} ORDER BY create_time DESC, id DESC LIMIT #{limit}")
    List<MessageItem> selectLatestBySessionId(@Param("sessionId") Long sessionId,
                                              @Param("limit") Integer limit);

    @Update("UPDATE message_items SET is_read = 1 WHERE session_id = #{sessionId} AND is_read = 0")
    int markSessionItemsRead(@Param("sessionId") Long sessionId);
}
