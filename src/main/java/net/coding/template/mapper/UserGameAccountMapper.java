package net.coding.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.coding.template.entity.po.UserGameAccount;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserGameAccountMapper extends BaseMapper<UserGameAccount> {
    @Select("SELECT * FROM user_game_accounts WHERE user_id = #{userId} ORDER BY update_time DESC")
    List<UserGameAccount> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_game_accounts WHERE id = #{id} AND user_id = #{userId}")
    UserGameAccount selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    @Delete("DELETE FROM user_game_accounts WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);
}
