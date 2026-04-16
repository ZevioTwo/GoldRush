package net.coding.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.coding.template.entity.po.BountyClaim;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BountyClaimMapper extends BaseMapper<BountyClaim> {

    @Select("SELECT * FROM bounty_claims WHERE bounty_id = #{bountyId} AND hunter_user_id = #{hunterUserId} LIMIT 1")
    BountyClaim selectByBountyAndHunter(@Param("bountyId") Long bountyId,
                                        @Param("hunterUserId") Long hunterUserId);
}
