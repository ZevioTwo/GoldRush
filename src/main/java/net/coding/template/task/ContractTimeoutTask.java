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
     * 处理超时未开始的契约（接单后30分钟未开始，每分钟执行一次）
     */
    @Scheduled(cron = "0 */1 * * * ?")
    @Transactional
    public void handleTimeoutContracts() {
        try {
            // 查询超时未开始的契约（接单30分钟后）
            List<Contract> timeoutContracts = contractMapper.selectTimeoutContracts();

            if (timeoutContracts.isEmpty()) {
                return;
            }

            log.info("发现接单超时契约: {}个", timeoutContracts.size());

            for (Contract contract : timeoutContracts) {
                try {
                    // 释放接单，回到大厅
                    contractMapper.releaseContract(contract.getId());

                    log.info("接单超时已释放: {}", contract.getContractNo());
                } catch (Exception e) {
                    log.error("处理接单超时契约失败: {}", contract.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("处理接单超时任务异常", e);
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