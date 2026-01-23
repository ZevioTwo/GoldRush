package net.coding.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.coding.template.entity.po.PaymentOrder;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    /**
     * 根据订单号查询
     */
    @Select("SELECT * FROM payment_orders WHERE order_no = #{orderNo}")
    PaymentOrder selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据微信商户订单号查询
     */
    @Select("SELECT * FROM payment_orders WHERE wx_out_trade_no = #{outTradeNo}")
    PaymentOrder selectByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    /**
     * 根据契约ID和订单类型查询
     */
    @Select("SELECT * FROM payment_orders WHERE contract_id = #{contractId} AND order_type = #{orderType}")
    List<PaymentOrder> selectByContractAndType(@Param("contractId") String contractId,
                                               @Param("orderType") String orderType);

    /**
     * 查询用户的支付订单
     */
    @Select("<script>" +
            "SELECT * FROM payment_orders WHERE user_id = #{userId} " +
            "<if test='orderType != null'>" +
            "   AND order_type = #{orderType} " +
            "</if>" +
            "<if test='paymentStatus != null'>" +
            "   AND payment_status = #{paymentStatus} " +
            "</if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<PaymentOrder> selectUserOrders(@Param("userId") Long userId,
                                        @Param("orderType") String orderType,
                                        @Param("paymentStatus") String paymentStatus,
                                        @Param("offset") Integer offset,
                                        @Param("limit") Integer limit);

    /**
     * 更新支付状态
     */
    @Update("UPDATE payment_orders SET payment_status = #{status}, " +
            "wx_transaction_id = #{transactionId}, pay_time = #{payTime}, " +
            "update_time = NOW() WHERE id = #{orderId}")
    int updatePaymentStatus(@Param("orderId") Long orderId,
                            @Param("status") String status,
                            @Param("transactionId") String transactionId,
                            @Param("payTime") LocalDateTime payTime);

    /**
     * 更新冻结信息
     */
    @Update("UPDATE payment_orders SET freeze_contract_id = #{freezeContractId}, " +
            "freeze_transaction_id = #{freezeTransactionId}, freeze_time = #{freezeTime}, " +
            "update_time = NOW() WHERE id = #{orderId}")
    int updateFreezeInfo(@Param("orderId") Long orderId,
                         @Param("freezeContractId") String freezeContractId,
                         @Param("freezeTransactionId") String freezeTransactionId,
                         @Param("freezeTime") LocalDateTime freezeTime);

    /**
     * 更新解冻信息
     */
    @Update("UPDATE payment_orders SET unfreeze_transaction_id = #{unfreezeTransactionId}, " +
            "unfreeze_time = #{unfreezeTime}, update_time = NOW() WHERE id = #{orderId}")
    int updateUnfreezeInfo(@Param("orderId") Long orderId,
                           @Param("unfreezeTransactionId") String unfreezeTransactionId,
                           @Param("unfreezeTime") LocalDateTime unfreezeTime);

    /**
     * 更新退款信息
     */
    @Update("UPDATE payment_orders SET refund_status = #{refundStatus}, " +
            "refund_time = #{refundTime}, update_time = NOW() WHERE id = #{orderId}")
    int updateRefundInfo(@Param("orderId") Long orderId,
                         @Param("refundStatus") String refundStatus,
                         @Param("refundTime") LocalDateTime refundTime);

    /**
     * 标记为已结算
     */
    @Update("UPDATE payment_orders SET is_settled = 1, update_time = NOW() WHERE id = #{orderId}")
    int markAsSettled(@Param("orderId") Long orderId);

    /**
     * 查询未结算的服务费订单
     */
    @Select("SELECT * FROM payment_orders WHERE order_type = 'SERVICE_FEE' " +
            "AND payment_status = 'SUCCESS' AND is_settled = 0")
    List<PaymentOrder> selectUnsettledServiceFee();

    /**
     * 统计契约相关订单金额
     */
    @Select("SELECT SUM(amount) FROM payment_orders " +
            "WHERE contract_id = #{contractId} AND user_id = #{userId} " +
            "AND payment_status = 'SUCCESS'")
    BigDecimal sumContractAmount(@Param("contractId") String contractId,
                                 @Param("userId") Long userId);

    // 前面已定义的方法保持不变，新增以下方法：

    /**
     * 根据契约ID查询所有支付订单
     */
    @Select("SELECT * FROM payment_orders WHERE contract_id = #{contractId} ORDER BY create_time DESC")
    List<PaymentOrder> selectByContractId(@Param("contractId") String contractId);

    /**
     * 查询待回调的支付订单
     */
    @Select("SELECT * FROM payment_orders WHERE callback_status = 'PENDING' " +
            "AND payment_status = 'SUCCESS' " +
            "AND create_time > DATE_SUB(NOW(), INTERVAL 7 DAY)")
    List<PaymentOrder> selectPendingCallbackOrders();

    /**
     * 更新回调状态
     */
    @Update("UPDATE payment_orders SET callback_status = #{callbackStatus}, " +
            "callback_count = callback_count + 1, " +
            "last_callback_time = #{callbackTime}, " +
            "update_time = NOW() " +
            "WHERE id = #{orderId}")
    int updateCallbackStatus(@Param("orderId") Long orderId,
                             @Param("callbackStatus") String callbackStatus,
                             @Param("callbackTime") LocalDateTime callbackTime);

    /**
     * 查询用户的押金冻结订单
     */
    @Select("SELECT * FROM payment_orders " +
            "WHERE user_id = #{userId} " +
            "AND order_type = 'DEPOSIT_FREEZE' " +
            "AND payment_status = 'SUCCESS' " +
            "AND contract_id = #{contractId}")
    PaymentOrder selectDepositFreezeOrder(@Param("userId") Long userId,
                                          @Param("contractId") String contractId);

    /**
     * 查询超时未支付的订单
     */
    @Select("SELECT * FROM payment_orders WHERE payment_status = 'PENDING' " +
            "AND expire_time < NOW()")
    List<PaymentOrder> selectExpiredOrders();

    /**
     * 标记订单为已过期
     */
    @Update("UPDATE payment_orders SET payment_status = 'CLOSED', " +
            "update_time = NOW() " +
            "WHERE id = #{orderId} AND payment_status = 'PENDING'")
    int markOrderAsExpired(@Param("orderId") Long orderId);

    /**
     * 查询需要重试回调的订单
     */
    @Select("SELECT * FROM payment_orders WHERE callback_status = 'FAILED' " +
            "AND callback_count < 5 " +
            "AND create_time > DATE_SUB(NOW(), INTERVAL 3 DAY)")
    List<PaymentOrder> selectRetryCallbackOrders();
}
