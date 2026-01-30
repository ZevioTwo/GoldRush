package net.coding.template.task;

import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.po.PaymentOrder;
import net.coding.template.mapper.PaymentOrderMapper;
import net.coding.template.service.PaymentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class PaymentTask {

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private PaymentService paymentService;

    /**
     * 处理过期支付订单（每分钟执行一次）
     */
    @Scheduled(cron = "0 */1 * * * ?")
    @Transactional
    public void handleExpiredOrders() {
        try {
            List<PaymentOrder> expiredOrders = paymentOrderMapper.selectExpiredOrders();

            if (expiredOrders.isEmpty()) {
                return;
            }

            log.info("发现过期支付订单: {}个", expiredOrders.size());

            for (PaymentOrder order : expiredOrders) {
                try {
                    // 标记为已关闭
                    paymentOrderMapper.markOrderAsExpired(order.getId());

                    // 如果是押金冻结订单，需要特殊处理
                    if ("DEPOSIT_FREEZE".equals(order.getOrderType())) {
                        // 更新契约冻结状态
                        // TODO: 实现契约状态更新
                    }

                    log.info("支付订单已过期关闭: orderNo={}", order.getOrderNo());
                } catch (Exception e) {
                    log.error("处理过期订单失败: {}", order.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("处理过期订单任务异常", e);
        }
    }

    /**
     * 重试失败的回调（每5分钟执行一次）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void retryFailedCallbacks() {
        try {
            List<PaymentOrder> retryOrders = paymentOrderMapper.selectRetryCallbackOrders();

            if (retryOrders.isEmpty()) {
                return;
            }

            log.info("发现需要重试回调的订单: {}个", retryOrders.size());

            for (PaymentOrder order : retryOrders) {
                try {
                    // 重新发送回调通知
                    // TODO: 实现回调重试逻辑

                    log.info("回调重试: orderNo={}, count={}",
                            order.getOrderNo(), order.getCallbackCount() + 1);
                } catch (Exception e) {
                    log.error("回调重试失败: {}", order.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("回调重试任务异常", e);
        }
    }
}
