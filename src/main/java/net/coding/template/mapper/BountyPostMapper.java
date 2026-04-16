package net.coding.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.coding.template.entity.po.BountyPost;
import net.coding.template.entity.response.BountyDetailResponse;
import net.coding.template.entity.response.BountyListResponse;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BountyPostMapper extends BaseMapper<BountyPost> {

    @Select("<script>" +
            "SELECT " +
            "b.id AS id, " +
            "b.bounty_no AS bountyNo, " +
            "b.title AS title, " +
            "b.target_role_name AS targetRoleName, " +
            "b.game_type AS gameType, " +
            "b.description AS description, " +
            "b.reward_mojin AS rewardMojin, " +
            "b.total_reward_mojin AS totalRewardMojin, " +
            "b.recruit_target_count AS recruitTargetCount, " +
            "b.recruit_current_count AS recruitCurrentCount, " +
            "b.creator_credit_score AS creatorCreditScore, " +
            "b.status AS status, " +
            "b.create_time AS createTime, " +
            "CASE WHEN bc.id IS NULL THEN 0 ELSE 1 END AS claimed, " +
            "CASE " +
            "  WHEN b.creator_user_id = #{currentUserId} THEN 0 " +
            "  WHEN bc.id IS NOT NULL THEN 0 " +
            "  WHEN b.status &lt;&gt; 'OPEN' THEN 0 " +
            "  WHEN b.recruit_current_count &gt;= b.recruit_target_count THEN 0 " +
            "  ELSE 1 " +
            "END AS canClaim " +
            "FROM bounty_posts b " +
            "LEFT JOIN bounty_claims bc ON bc.bounty_id = b.id " +
            "AND bc.hunter_user_id = #{currentUserId} " +
            "AND bc.status = 'CLAIMED' " +
            "WHERE b.status IN ('OPEN', 'FULL') " +
            "<if test='gameType != null and gameType != \"\"'>" +
            "  AND b.game_type = #{gameType} " +
            "</if> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (b.title LIKE CONCAT('%', #{keyword}, '%') " +
            "    OR b.target_role_name LIKE CONCAT('%', #{keyword}, '%') " +
            "    OR b.game_type LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if> " +
            "ORDER BY b.creator_credit_score DESC, b.reward_mojin DESC, b.create_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<BountyListResponse.BountyItem> selectBountyList(@Param("currentUserId") Long currentUserId,
                                                         @Param("gameType") String gameType,
                                                         @Param("keyword") String keyword,
                                                         @Param("offset") Integer offset,
                                                         @Param("limit") Integer limit);

    @Select("<script>" +
            "SELECT COUNT(1) FROM bounty_posts b " +
            "WHERE b.status IN ('OPEN', 'FULL') " +
            "<if test='gameType != null and gameType != \"\"'>" +
            "  AND b.game_type = #{gameType} " +
            "</if> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (b.title LIKE CONCAT('%', #{keyword}, '%') " +
            "    OR b.target_role_name LIKE CONCAT('%', #{keyword}, '%') " +
            "    OR b.game_type LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "</script>")
    Long countBountyList(@Param("gameType") String gameType,
                         @Param("keyword") String keyword);

    @Select("SELECT " +
            "b.id AS id, " +
            "b.bounty_no AS bountyNo, " +
            "b.title AS title, " +
            "b.target_role_name AS targetRoleName, " +
            "b.game_type AS gameType, " +
            "b.description AS description, " +
            "b.reward_mojin AS rewardMojin, " +
            "b.total_reward_mojin AS totalRewardMojin, " +
            "b.recruit_target_count AS recruitTargetCount, " +
            "b.recruit_current_count AS recruitCurrentCount, " +
            "b.creator_credit_score AS creatorCreditScore, " +
            "b.status AS status, " +
            "b.create_time AS createTime, " +
            "CASE WHEN bc.id IS NULL THEN 0 ELSE 1 END AS claimed, " +
            "CASE " +
            "  WHEN b.creator_user_id = #{currentUserId} THEN 0 " +
            "  WHEN bc.id IS NOT NULL THEN 0 " +
            "  WHEN b.status <> 'OPEN' THEN 0 " +
            "  WHEN b.recruit_current_count >= b.recruit_target_count THEN 0 " +
            "  ELSE 1 " +
            "END AS canClaim " +
            "FROM bounty_posts b " +
            "LEFT JOIN bounty_claims bc ON bc.bounty_id = b.id " +
            "AND bc.hunter_user_id = #{currentUserId} " +
            "AND bc.status = 'CLAIMED' " +
            "WHERE b.id = #{bountyId} " +
            "LIMIT 1")
    BountyDetailResponse selectBountyDetail(@Param("currentUserId") Long currentUserId,
                                            @Param("bountyId") Long bountyId);

    @Update("UPDATE bounty_posts SET recruit_current_count = recruit_current_count + 1, " +
            "status = CASE WHEN recruit_current_count + 1 >= recruit_target_count THEN 'FULL' ELSE 'OPEN' END, " +
            "update_time = NOW() " +
            "WHERE id = #{bountyId} AND status = 'OPEN' AND recruit_current_count < recruit_target_count")
    int incrementClaimCount(@Param("bountyId") Long bountyId);
}
