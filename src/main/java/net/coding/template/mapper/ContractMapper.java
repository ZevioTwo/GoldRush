package net.coding.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.coding.template.entity.po.Contract;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ContractMapper extends BaseMapper<Contract> {

    /**
     * 根据ID查询契约详情
     */
    @Select("SELECT * FROM contracts WHERE id = #{id}")
    Contract selectContractDetail(@Param("id") String id);

    /**
     * 根据契约编号查询
     */
    @Select("SELECT * FROM contracts WHERE contract_no = #{contractNo}")
    Contract selectByContractNo(@Param("contractNo") String contractNo);

    /**
     * 查询用户相关的契约列表
     */
    @Select("<script>" +
            "SELECT * FROM contracts " +
            "WHERE (initiator_id = #{userId} OR receiver_id = #{userId}) " +
            "<if test='status != null'>" +
            "   AND status = #{status} " +
            "</if>" +
            "<if test='gameType != null'>" +
            "   AND game_type = #{gameType} " +
            "</if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Contract> selectUserContracts(@Param("userId") Long userId,
                                       @Param("status") String status,
                                       @Param("gameType") String gameType,
                                       @Param("offset") Integer offset,
                                       @Param("limit") Integer limit);

    /**
     * 统计用户契约数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM contracts " +
            "WHERE (initiator_id = #{userId} OR receiver_id = #{userId}) " +
            "<if test='status != null'>" +
            "   AND status = #{status} " +
            "</if>" +
            "</script>")
    int countUserContracts(@Param("userId") Long userId,
                           @Param("status") String status);

    /**
     * 更新契约状态
     */
    @Update("UPDATE contracts SET status = #{status}, update_time = NOW() WHERE id = #{contractId}")
    int updateContractStatus(@Param("contractId") String contractId,
                             @Param("status") String status);

    /**
     * 更新契约阶段
     */
    @Update("UPDATE contracts SET phase = #{phase}, update_time = NOW() WHERE id = #{contractId}")
    int updateContractPhase(@Param("contractId") String contractId,
                            @Param("phase") String phase);

    /**
     * 开始契约（更新开始时间）
     */
    @Update("UPDATE contracts SET phase = 'IN_GAME', start_time = #{startTime}, " +
            "update_time = NOW() WHERE id = #{contractId} AND status = 'ACTIVE'")
    int startContract(@Param("contractId") String contractId,
                      @Param("startTime") LocalDateTime startTime);

    /**
     * 完成契约
     */
    @Update("UPDATE contracts SET status = 'COMPLETED', phase = 'SETTLEMENT', " +
            "end_time = #{endTime}, complete_time = NOW(), " +
            "update_time = NOW() WHERE id = #{contractId} AND status = 'ACTIVE'")
    int completeContract(@Param("contractId") String contractId,
                         @Param("endTime") LocalDateTime endTime);

    /**
     * 取消契约
     */
    @Update("UPDATE contracts SET status = 'CANCELLED', cancel_time = NOW(), " +
            "update_time = NOW() WHERE id = #{contractId} AND status = 'PAID'")
    int cancelContract(@Param("contractId") String contractId);

    /**
     * 标记为违约
     */
    @Update("UPDATE contracts SET status = 'VIOLATED', violate_time = NOW(), " +
            "update_time = NOW() WHERE id = #{contractId}")
    int markAsViolated(@Param("contractId") String contractId);

    /**
     * 进入争议状态
     */
    @Update("UPDATE contracts SET status = 'DISPUTE', update_time = NOW() WHERE id = #{contractId}")
    int enterDispute(@Param("contractId") String contractId);

    /**
     * 更新支付状态
     */
    @Update("UPDATE contracts SET payment_status = #{paymentStatus}, update_time = NOW() WHERE id = #{contractId}")
    int updatePaymentStatus(@Param("contractId") String contractId,
                            @Param("paymentStatus") String paymentStatus);

    /**
     * 更新冻结状态
     */
    @Update("UPDATE contracts SET freeze_status = #{freezeStatus}, update_time = NOW() WHERE id = #{contractId}")
    int updateFreezeStatus(@Param("contractId") String contractId,
                           @Param("freezeStatus") String freezeStatus);

    /**
     * 检查用户是否有进行中的契约
     */
    @Select("SELECT COUNT(*) FROM contracts " +
            "WHERE (initiator_id = #{userId} OR receiver_id = #{userId}) " +
            "AND status IN ('PAID', 'ACTIVE', 'IN_GAME')")
    int countActiveContracts(@Param("userId") Long userId);

    /**
     * 查询待确认的契约（对方已确认，等待本方确认）
     */
    @Select("SELECT c.* FROM contracts c " +
            "JOIN contract_confirms cc ON c.id = cc.contract_id " +
            "WHERE (c.initiator_id = #{userId} OR c.receiver_id = #{userId}) " +
            "AND c.status = 'ACTIVE' " +
            "AND cc.confirm_status = 'WAITING_OTHER' " +
            "AND cc.user_id != #{userId}")
    List<Contract> selectPendingConfirmContracts(@Param("userId") Long userId);

    /**
     * 查询超时未开始的契约
     */
    @Select("SELECT * FROM contracts WHERE status = 'PAID' " +
            "AND create_time < DATE_SUB(NOW(), INTERVAL 30 MINUTE)")
    List<Contract> selectTimeoutContracts();

    /**
     * 更新契约退款状态
     */
    @Update("UPDATE contracts SET refund_status = #{refundStatus}, update_time = NOW() WHERE id = #{contractId}")
    int updateRefundStatus(@Param("contractId") String contractId,
                           @Param("refundStatus") String refundStatus);
}
