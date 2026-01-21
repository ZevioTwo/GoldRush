package net.coding.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.coding.template.entity.dto.po.ContractConfirm;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ContractConfirmMapper extends BaseMapper<ContractConfirm> {

    /**
     * 根据契约和用户查询确认记录
     */
    @Select("SELECT * FROM contract_confirms WHERE contract_id = #{contractId} AND user_id = #{userId}")
    ContractConfirm selectByContractAndUser(@Param("contractId") String contractId,
                                            @Param("userId") Long userId);

    /**
     * 更新确认状态
     */
    @Update("UPDATE contract_confirms SET confirm_status = #{status}, " +
            "confirm_time = #{confirmTime}, update_time = NOW() " +
            "WHERE id = #{id}")
    int updateConfirmStatus(@Param("id") Long id,
                            @Param("status") String status,
                            @Param("confirmTime") LocalDateTime confirmTime);

    /**
     * 查询契约的确认状态
     */
    @Select("SELECT * FROM contract_confirms WHERE contract_id = #{contractId} ORDER BY user_id")
    List<ContractConfirm> selectContractConfirms(@Param("contractId") String contractId);

    /**
     * 统计契约的确认数量
     */
    @Select("SELECT COUNT(*) FROM contract_confirms " +
            "WHERE contract_id = #{contractId} AND confirm_status = 'CONFIRMED'")
    int countConfirmed(@Param("contractId") String contractId);
}
