package net.coding.template.service;

import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.enums.OrderType;
import net.coding.template.entity.enums.PaymentStatus;
import net.coding.template.entity.po.Contract;
import net.coding.template.entity.po.PaymentOrder;
import net.coding.template.entity.po.User;
import net.coding.template.entity.request.PaymentRequest;
import net.coding.template.entity.response.PaymentResponse;
import net.coding.template.exception.BusinessException;
import net.coding.template.mapper.ContractMapper;
import net.coding.template.mapper.PaymentOrderMapper;
import net.coding.template.util.OrderNoGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class PaymentService {

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private ContractMapper contractMapper;

    @Resource
    private UserService userService;

    @Resource
    private WeChatPayService weChatPayService;

    @Resource
    private OrderNoGenerator orderNoGenerator;

    @Resource
    private ContractService contractService;

    /**
     * 创建支付订单
     */
    @Transactional
    public PaymentResponse createPaymentOrder(PaymentRequest request, String token) {
        // 1. 获取当前用户
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 2. 验证契约
        Contract contract = contractMapper.selectContractDetail(request.getContractId());
        if (contract == null) {
            throw new BusinessException(404, "契约不存在");
        }

        // 3. 验证用户是否是参与方
        if (!contract.getInitiatorId().equals(currentUser.getId())
                && !contract.getReceiverId().equals(currentUser.getId())) {
            throw new BusinessException(403, "无权支付此契约");
        }

        // 4. 验证契约状态
        if (!"PENDING".equals(contract.getStatus())) {
            throw new BusinessException(400, "契约状态不允许支付");
        }

        // 5. 检查是否已支付过
        List<PaymentOrder> existingOrders = paymentOrderMapper.selectByContractAndType(
                request.getContractId(), request.getOrderType());

        for (PaymentOrder order : existingOrders) {
            if (order.getUserId().equals(currentUser.getId())
                    && "SUCCESS".equals(order.getPaymentStatus())) {
                throw new BusinessException(400, "您已经支付过此费用");
            }
        }

        // 6. 创建支付订单
        PaymentOrder paymentOrder = createOrderRecord(request, currentUser, contract);

        // 7. 调用微信支付
        Map<String, String> payParams;
        if ("DEPOSIT_FREEZE".equals(request.getOrderType())) {
            // 资金冻结
            payParams = weChatPayService.freezeDeposit(paymentOrder, currentUser.getOpenid());

            // 保存冻结协议号
            if (payParams.containsKey("freeze_contract_id")) {
                paymentOrder.setFreezeContractId(payParams.get("freeze_contract_id"));
                paymentOrderMapper.updateById(paymentOrder);
            }
        } else {
            // 普通支付（服务费、VIP等）
            payParams = weChatPayService.unifiedOrder(paymentOrder, currentUser.getOpenid());
        }

        // 8. 构建响应
        PaymentResponse response = new PaymentResponse();
        response.setOrderId(paymentOrder.getId());
        response.setOrderNo(paymentOrder.getOrderNo());
        response.setAmount(paymentOrder.getAmount());
        response.setPayParams(payParams);
        response.setExpireTime(paymentOrder.getExpireTime());

        log.info("支付订单创建成功: {}, 类型: {}", paymentOrder.getOrderNo(), request.getOrderType());

        return response;
    }

    /**
     * 处理支付回调
     */
    @Transactional
    public void handlePaymentNotify(String notifyBody) {
        try {
            // 1. 验证回调签名
            // 这里需要从请求头获取签名信息，简化处理

            // 2. 解析回调数据
            Map<String, String> notifyResult = weChatPayService.handlePaymentNotify(notifyBody);

            String outTradeNo = notifyResult.get("out_trade_no");
            String transactionId = notifyResult.get("transaction_id");
            String tradeState = notifyResult.get("trade_state");

            // 3. 查询订单
            PaymentOrder paymentOrder = paymentOrderMapper.selectByOutTradeNo(outTradeNo);
            if (paymentOrder == null) {
                log.error("支付回调订单不存在: {}", outTradeNo);
                return;
            }

            // 4. 更新订单状态
            if ("SUCCESS".equals(tradeState)) {
                paymentOrder.setPaymentStatus(PaymentStatus.SUCCESS.getCode());
                paymentOrder.setWxTransactionId(transactionId);
                paymentOrder.setPayTime(LocalDateTime.now());
                paymentOrderMapper.updateById(paymentOrder);

                // 5. 根据订单类型处理后续逻辑
                handleOrderAfterPayment(paymentOrder);

                log.info("支付成功处理完成: {}", outTradeNo);
            } else {
                log.warn("支付未成功: {}, 状态: {}", outTradeNo, tradeState);
            }
        } catch (Exception e) {
            log.error("处理支付回调异常", e);
            throw new BusinessException(500, "支付回调处理失败");
        }
    }

    /**
     * 解冻押金
     */
    @Transactional
    public void unfreezeDeposit(String contractId) {
        try {
            // 1. 查询契约的押金冻结订单
            List<PaymentOrder> freezeOrders = paymentOrderMapper.selectByContractAndType(
                    contractId, OrderType.DEPOSIT_FREEZE.getCode());

            for (PaymentOrder freezeOrder : freezeOrders) {
                if (!"SUCCESS".equals(freezeOrder.getPaymentStatus())) {
                    continue;
                }

                // 2. 创建解冻订单
                PaymentOrder unfreezeOrder = new PaymentOrder();
                unfreezeOrder.setOrderNo(orderNoGenerator.generate("UNFREEZE"));
                unfreezeOrder.setContractId(contractId);
                unfreezeOrder.setUserId(freezeOrder.getUserId());
                unfreezeOrder.setOrderType(OrderType.DEPOSIT_UNFREEZE.getCode());
                unfreezeOrder.setAmount(freezeOrder.getAmount());
                unfreezeOrder.setPaymentStatus(PaymentStatus.PENDING.getCode());
                unfreezeOrder.setExpireTime(LocalDateTime.now().plusDays(7));
                paymentOrderMapper.insert(unfreezeOrder);

                // 3. 调用微信解冻接口
                boolean success = weChatPayService.unfreezeDeposit(freezeOrder);
                if (success) {
                    // 更新解冻订单状态
                    unfreezeOrder.setPaymentStatus(PaymentStatus.SUCCESS.getCode());
                    unfreezeOrder.setUnfreezeTime(LocalDateTime.now());
                    paymentOrderMapper.updateById(unfreezeOrder);

                    // 更新冻结订单的解冻信息
                    freezeOrder.setUnfreezeTransactionId(unfreezeOrder.getOrderNo());
                    freezeOrder.setUnfreezeTime(LocalDateTime.now());
                    paymentOrderMapper.updateById(freezeOrder);

                    log.info("押金解冻成功: 用户={}, 金额={}",
                            freezeOrder.getUserId(), freezeOrder.getAmount());
                } else {
                    log.error("押金解冻失败: 用户={}", freezeOrder.getUserId());
                    // 可以记录失败，后续手动处理
                }
            }
        } catch (Exception e) {
            log.error("解冻押金异常", e);
            throw new BusinessException(500, "解冻押金失败");
        }
    }

    /**
     * 扣除违约金
     */
    @Transactional
    public void deductPenalty(String contractId, Long violatorUserId, Long victimUserId, BigDecimal amount) {
        try {
            // 1. 查询违约者的押金冻结订单
            List<PaymentOrder> freezeOrders = paymentOrderMapper.selectByContractAndType(
                    contractId, OrderType.DEPOSIT_FREEZE.getCode());

            PaymentOrder violatorFreezeOrder = freezeOrders.stream()
                    .filter(order -> order.getUserId().equals(violatorUserId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(404, "未找到押金冻结记录"));

            // 2. 创建扣除订单
            PaymentOrder deductOrder = new PaymentOrder();
            deductOrder.setOrderNo(orderNoGenerator.generate("DEDUCT"));
            deductOrder.setContractId(contractId);
            deductOrder.setUserId(violatorUserId);
            deductOrder.setOrderType(OrderType.DEPOSIT_DEDUCT.getCode());
            deductOrder.setAmount(amount);
            deductOrder.setPaymentStatus(PaymentStatus.PENDING.getCode());
            paymentOrderMapper.insert(deductOrder);

            // 3. 调用微信扣款接口
            boolean success = weChatPayService.deductPenalty(violatorFreezeOrder);
            if (success) {
                // 更新扣除订单状态
                deductOrder.setPaymentStatus(PaymentStatus.SUCCESS.getCode());
                deductOrder.setPayTime(LocalDateTime.now());
                paymentOrderMapper.updateById(deductOrder);

                // 4. 创建受害者的收款订单（模拟）
                PaymentOrder victimOrder = new PaymentOrder();
                victimOrder.setOrderNo(orderNoGenerator.generate("COMPENSATE"));
                victimOrder.setContractId(contractId);
                victimOrder.setUserId(victimUserId);
                victimOrder.setOrderType("COMPENSATION");
                victimOrder.setAmount(amount.subtract(amount.multiply(new BigDecimal("0.01")))); // 扣除1%手续费
                victimOrder.setPaymentStatus(PaymentStatus.SUCCESS.getCode());
                victimOrder.setPayTime(LocalDateTime.now());
                paymentOrderMapper.insert(victimOrder);

                log.info("违约金扣除成功: 违约者={}, 受害者={}, 金额={}",
                        violatorUserId, victimUserId, amount);
            } else {
                log.error("违约金扣除失败: 违约者={}", violatorUserId);
                throw new BusinessException(500, "违约金扣除失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("扣除违约金异常", e);
            throw new BusinessException(500, "扣除违约金失败");
        }
    }

    /**
     * 检查契约支付状态
     */
    public boolean checkContractPayment(String contractId) {
        // 1. 查询契约
        Contract contract = contractMapper.selectContractDetail(contractId);
        if (contract == null) {
            return false;
        }

        // 2. 检查服务费支付状态
        List<PaymentOrder> serviceFeeOrders = paymentOrderMapper.selectByContractAndType(
                contractId, OrderType.SERVICE_FEE.getCode());

        boolean serviceFeePaid = serviceFeeOrders.stream()
                .allMatch(order -> "SUCCESS".equals(order.getPaymentStatus()));

        // 3. 检查押金冻结状态
        List<PaymentOrder> depositOrders = paymentOrderMapper.selectByContractAndType(
                contractId, OrderType.DEPOSIT_FREEZE.getCode());

        boolean depositFrozen = depositOrders.stream()
                .allMatch(order -> "SUCCESS".equals(order.getPaymentStatus()));

        // 4. 更新契约支付状态
        if (serviceFeePaid && depositFrozen) {
            contractMapper.updatePaymentStatus(contractId, "FULL");
            contractMapper.updateContractStatus(contractId, "PAID");
            return true;
        } else if (serviceFeePaid || depositFrozen) {
            contractMapper.updatePaymentStatus(contractId, "PARTIAL");
            return false;
        } else {
            return false;
        }
    }

    /**
     * 退款处理
     */
    public void refundIfPaid(String contractId) {
        // 查询已支付的订单并进行退款
        List<PaymentOrder> paidOrders = paymentOrderMapper.selectByContractAndType(contractId, null);

        for (PaymentOrder order : paidOrders) {
            if ("SUCCESS".equals(order.getPaymentStatus()) && "NONE".equals(order.getRefundStatus())) {
                // 执行退款逻辑
                processRefund(order);
            }
        }
    }

    // ============ 私有方法 ============

    private PaymentOrder createOrderRecord(PaymentRequest request, User user, Contract contract) {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setOrderNo(orderNoGenerator.generate(request.getOrderType()));
        paymentOrder.setContractId(request.getContractId());
        paymentOrder.setUserId(user.getId());
        paymentOrder.setOrderType(request.getOrderType());

        // 设置金额
        BigDecimal amount = calculateAmount(request, contract, user);
        paymentOrder.setAmount(amount);

        // 计算实际金额（扣除手续费前）
        paymentOrder.setActualAmount(amount);

        // 微信商户订单号
        paymentOrder.setWxOutTradeNo(generateOutTradeNo());

        // 设置过期时间
        paymentOrder.setExpireTime(LocalDateTime.now().plusMinutes(30));

        // 保存到数据库
        paymentOrderMapper.insert(paymentOrder);

        return paymentOrder;
    }

    private BigDecimal calculateAmount(PaymentRequest request, Contract contract, User user) {
        switch (request.getOrderType()) {
            case "SERVICE_FEE":
                // VIP用户免费
                if (Boolean.TRUE.equals(user.getIsVip())) {
                    return BigDecimal.ZERO;
                }
                return contract.getServiceFeeAmount();

            case "DEPOSIT_FREEZE":
                return contract.getDepositAmount();

            case "VIP_PAYMENT":
                // 从配置或请求中获取VIP价格
                return request.getAmount() != null ? request.getAmount() : new BigDecimal("9.90");

            case "ARBITRATION_FEE":
                return new BigDecimal("5.00");

            default:
                throw new BusinessException(400, "不支持的订单类型");
        }
    }

    private String generateOutTradeNo() {
        return "WX" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    private void handleOrderAfterPayment(PaymentOrder paymentOrder) {
        switch (paymentOrder.getOrderType()) {
            case "SERVICE_FEE":
                // 服务费支付成功，检查契约是否可开始
                checkAndStartContract(paymentOrder.getContractId());
                break;

            case "DEPOSIT_FREEZE":
                // 押金冻结成功，检查契约是否可开始
                checkAndStartContract(paymentOrder.getContractId());
                break;

            case "VIP_PAYMENT":
                // VIP支付成功，更新用户VIP状态
                updateUserVipStatus(paymentOrder.getUserId());
                break;

            case "ARBITRATION_FEE":
                // 仲裁费支付成功，更新仲裁状态
                updateArbitrationStatus(paymentOrder);
                break;
        }
    }

    private void checkAndStartContract(String contractId) {
        // 检查契约支付是否完成
        boolean allPaid = checkContractPayment(contractId);

        if (allPaid) {
            // 所有费用已支付，开始契约
            contractService.startContractAfterPayment(contractId);
        }
    }

    private void updateUserVipStatus(Long userId) {
        // 更新用户VIP状态
        // 实现逻辑...
    }

    private void updateArbitrationStatus(PaymentOrder paymentOrder) {
        // 更新仲裁状态为加急处理
        // 实现逻辑...
    }

    private void processRefund(PaymentOrder order) {
        // 实现退款逻辑
        // 这里简化处理
        order.setRefundStatus("FULL");
        order.setRefundTime(LocalDateTime.now());
        paymentOrderMapper.updateById(order);

        log.info("订单退款成功: {}", order.getOrderNo());
    }
}
