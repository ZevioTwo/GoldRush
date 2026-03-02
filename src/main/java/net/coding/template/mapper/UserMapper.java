package net.coding.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.coding.template.entity.po.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据openid查询用户
     */
    @Select("SELECT * FROM users WHERE openid = #{openid} AND status = 'ACTIVE'")
    User selectByOpenid(@Param("openid") String openid);


    /**
     * 更新用户信用分
     */
    @Update("UPDATE users SET credit_score = #{creditScore}, update_time = NOW() WHERE id = #{userId}")
    int updateCreditScore(@Param("userId") Long userId, @Param("creditScore") Integer creditScore);

    /**
     * 增加用户总契约数
     */
    @Update("UPDATE users SET total_contracts = total_contracts + 1, update_time = NOW() WHERE id = #{userId}")
    int incrementTotalContracts(@Param("userId") Long userId);

    /**
     * 增加已完成契约数
     */
    @Update("UPDATE users SET completed_contracts = completed_contracts + 1, update_time = NOW() WHERE id = #{userId}")
    int incrementCompletedContracts(@Param("userId") Long userId);

    /**
     * 增加争议次数
     */
    @Update("UPDATE users SET dispute_count = dispute_count + 1, update_time = NOW() WHERE id = #{userId}")
    int incrementDisputeCount(@Param("userId") Long userId);

    /**
     * 增加违约次数并扣除信用分
     */
    @Update("UPDATE users SET violation_count = violation_count + 1, " +
            "credit_score = GREATEST(0, credit_score - #{deductScore}), " +
            "update_time = NOW() WHERE id = #{userId}")
    int incrementViolation(@Param("userId") Long userId, @Param("deductScore") Integer deductScore);

    /**
     * 更新VIP信息
     */
    @Update("UPDATE users SET is_vip = #{isVip}, vip_type = #{vipType}, " +
            "vip_start_time = #{startTime}, vip_expire_time = #{expireTime}, " +
            "update_time = NOW() WHERE id = #{userId}")
    int updateVipInfo(@Param("userId") Long userId,
                      @Param("isVip") Boolean isVip,
                      @Param("vipType") String vipType,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("expireTime") LocalDateTime expireTime);

    /**
     * 更新用户状态
     */
    @Update("UPDATE users SET status = #{status}, blacklist_reason = #{reason}, update_time = NOW() WHERE id = #{userId}")
    int updateUserStatus(@Param("userId") Long userId,
                         @Param("status") String status,
                         @Param("reason") String reason);

    /**
     * 更新用户联系方式
     */
    @Update("UPDATE users SET wechat_id = #{wechatId}, phone = #{phone}, update_time = NOW() WHERE id = #{userId}")
    int updateContactInfo(@Param("userId") Long userId,
                          @Param("wechatId") String wechatId,
                          @Param("phone") String phone);
}
