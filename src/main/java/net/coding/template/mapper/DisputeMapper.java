package net.coding.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.coding.template.entity.po.Dispute;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DisputeMapper extends BaseMapper<Dispute> {

    @Select("SELECT * FROM disputes WHERE dispute_no = #{disputeNo}")
    Dispute selectByDisputeNo(@Param("disputeNo") String disputeNo);

    @Select("SELECT * FROM disputes WHERE contract_id = #{contractId} ORDER BY create_time DESC")
    List<Dispute> selectByContractId(@Param("contractId") String contractId);

    @Update("UPDATE disputes SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE disputes SET status = #{status}, result = #{result}, result_reason = #{reason}, " +
            "handler_id = #{handlerId}, handle_time = NOW(), update_time = NOW() WHERE id = #{id}")
    int updateJudgeResult(@Param("id") Long id,
                          @Param("status") String status,
                          @Param("result") String result,
                          @Param("reason") String reason,
                          @Param("handlerId") Long handlerId);
}
