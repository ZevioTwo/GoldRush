package net.coding.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.coding.template.entity.dto.CreditRankingItemDTO;
import net.coding.template.entity.po.User;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
     * 增加摸金币余额
     */
    @Update("UPDATE users SET mojin_balance = mojin_balance + #{amount}, update_time = NOW() WHERE id = #{userId}")
    int incrementMojinBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 扣减摸金币余额
     */
    @Update("UPDATE users SET mojin_balance = mojin_balance - #{amount}, update_time = NOW() " +
            "WHERE id = #{userId} AND mojin_balance >= #{amount}")
    int deductMojinBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 摸金币兑换信誉分
     */
    @Update("UPDATE users SET mojin_balance = mojin_balance - #{mojinAmount}, " +
            "credit_score = credit_score + #{creditAmount}, " +
            "update_time = NOW() " +
            "WHERE id = #{userId} AND mojin_balance >= #{mojinAmount}")
    int exchangeMojinToCredit(@Param("userId") Long userId,
                              @Param("mojinAmount") BigDecimal mojinAmount,
                              @Param("creditAmount") Integer creditAmount);

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

    /**
     * 查询信誉红榜
     */
    @Select("SELECT " +
            "id AS userId, " +
            "COALESCE(NULLIF(TRIM(nickname), ''), CONCAT('用户', id)) AS name, " +
            "avatar_url AS avatarUrl, " +
            "credit_score AS score, " +
            "status AS status, " +
            "total_contracts AS totalContracts, " +
            "completed_contracts AS completedContracts, " +
            "violation_count AS violationCount " +
            "FROM users " +
            "ORDER BY credit_score DESC, completed_contracts DESC, total_contracts DESC, id ASC " +
            "LIMIT #{limit}")
    List<CreditRankingItemDTO> selectTopCreditRanking(@Param("limit") int limit);

    /**
     * 查询信誉黑榜
     */
    @Select("SELECT " +
            "id AS userId, " +
            "COALESCE(NULLIF(TRIM(nickname), ''), CONCAT('用户', id)) AS name, " +
            "avatar_url AS avatarUrl, " +
            "credit_score AS score, " +
            "status AS status, " +
            "total_contracts AS totalContracts, " +
            "completed_contracts AS completedContracts, " +
            "violation_count AS violationCount " +
            "FROM users " +
            "ORDER BY credit_score ASC, violation_count DESC, total_contracts DESC, id ASC " +
            "LIMIT #{limit}")
    List<CreditRankingItemDTO> selectBottomCreditRanking(@Param("limit") int limit);
}
