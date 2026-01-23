package net.coding.template.task;

import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.po.Contract;
import net.coding.template.mapper.ContractMapper;
import net.coding.template.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class ContractTimeoutTask {

    @Resource
    private ContractMapper contractMapper;

    @Resource
    private PaymentService paymentService;

    /**
     * 处理超时未支付的契约（每分钟执行一次）
     */
    @Scheduled(cron = "0 */1 * * * ?")
    @Transactional
    public void handleTimeoutContracts() {
        try {
            // 查询超时未支付的契约（创建30分钟后）
            List<Contract> timeoutContracts = contractMapper.selectTimeoutContracts();

            if (timeoutContracts.isEmpty()) {
                return;
            }

            log.info("发现超时未支付契约: {}个", timeoutContracts.size());

            for (Contract contract : timeoutContracts) {
                try {
                    // 更新状态为已取消
                    contractMapper.cancelContract(contract.getId());

                    // 如果有支付记录，进行退款
                    paymentService.refundIfPaid(contract.getId());

                    log.info("超时契约已取消: {}", contract.getContractNo());
                } catch (Exception e) {
                    log.error("处理超时契约失败: {}", contract.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("处理超时契约任务异常", e);
        }
    }

    /**
     * 自动完成超时未确认的契约（每小时执行一次）
     */
    @Scheduled(cron = "0 0 */1 * * ?")
    public void autoCompleteContracts() {
        // TODO: 实现自动完成逻辑
        // 对于ACTIVE状态超过72小时的契约，自动完成并解冻押金
    }
}