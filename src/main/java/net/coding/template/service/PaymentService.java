package net.coding.template.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.coding.template.configuration.WeChatPayConfig;
import net.coding.template.entity.enums.ContractStatus;
import net.coding.template.entity.enums.OrderType;
import net.coding.template.entity.enums.PaymentStatus;
import net.coding.template.entity.po.Contract;
import net.coding.template.entity.po.MojinLedger;
import net.coding.template.entity.po.PaymentOrder;
import net.coding.template.entity.po.User;
import net.coding.template.entity.request.*;
import net.coding.template.entity.response.DeductResponse;
import net.coding.template.entity.response.FreezeResponse;
import net.coding.template.entity.response.PaymentResponse;
import net.coding.template.entity.response.UnfreezeResponse;
import net.coding.template.exception.BusinessException;
import net.coding.template.mapper.ContractMapper;
import net.coding.template.mapper.MojinLedgerMapper;
import net.coding.template.mapper.PaymentOrderMapper;
import net.coding.template.mapper.UserMapper;
import net.coding.template.util.OrderNoGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentService {

    private static final BigDecimal RMB_TO_MOJIN_RATE = new BigDecimal("10");

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private ContractMapper contractMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    @Resource
    private WeChatPayService weChatPayService;

    @Resource
    private OrderNoGenerator orderNoGenerator;

    @Resource
    private SystemConfigService configService;

    @Resource
    private RedisService redisService;

    @Resource
    private WeChatPayConfig weChatPayConfig;

    @Resource
    private MojinLedgerMapper mojinLedgerMapper;

    /**
     * 1. 生成支付预订单
     */
    @Transactional
    public PaymentResponse prepay(PaymentRequest request, String token) {
        try {
            // 1. 获取当前用户
            User currentUser = userService.getCurrentUser(token);
            if (currentUser == null) {
                throw new BusinessException(401, "用户未登录");
            }

            if (OrderType.CREDIT_RECHARGE.getCode().equals(request.getOrderType())
                    && !StringUtils.hasText(request.getContractId())) {
                request.setContractId("MOJIN_RECHARGE_" + currentUser.getId());
            }

            // 2. 验证请求参数
            validatePaymentRequest(request, currentUser);

            // 3. 创建支付订单记录
            PaymentOrder paymentOrder = createPaymentOrder(request, currentUser);

            // 4. 调用微信支付接口生成预支付参数
            Map<String, String> payParams = generatePayParams(paymentOrder, currentUser);

            // 5. 构建响应
            PaymentResponse response = new PaymentResponse();
            response.setSuccess(true);
            response.setOrderId(paymentOrder.getId());
            response.setOrderNo(paymentOrder.getOrderNo());
            response.setContractId(paymentOrder.getContractId());
            response.setOrderType(paymentOrder.getOrderType());
            response.setAmount(paymentOrder.getAmount());
            response.setPayParams(payParams);
            response.setExpireTime(paymentOrder.getExpireTime());
            response.setMessage("预支付订单创建成功");

            log.info("预支付订单创建成功: orderNo={}, amount={}, type={}",
                    paymentOrder.getOrderNo(), paymentOrder.getAmount(), paymentOrder.getOrderType());

            return response;

        } catch (BusinessException e) {
            log.error("生成预支付订单失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("生成预支付订单异常", e);
            throw new BusinessException(500, "生成预支付订单失败");
        }
    }

    /**
     * 1.5 查询支付状态
     */
    @Transactional
    public Map<String, Object> getPaymentStatus(String orderNo, String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        PaymentOrder paymentOrder = paymentOrderMapper.selectByOrderNo(orderNo);
        if (paymentOrder == null) {
            throw new BusinessException(404, "支付订单不存在");
        }
        if (!paymentOrder.getUserId().equals(currentUser.getId())) {
            throw new BusinessException(403, "无权查看该支付订单");
        }

        String currentStatus = paymentOrder.getPaymentStatus();
        if (PaymentStatus.SUCCESS.getCode().equals(currentStatus)
                || PaymentStatus.CLOSED.getCode().equals(currentStatus)
                || PaymentStatus.FAILED.getCode().equals(currentStatus)) {
            return buildPaymentStatusResult(paymentOrder, currentStatus, null);
        }

        if (!StringUtils.hasText(paymentOrder.getWxOutTradeNo())) {
            return buildPaymentStatusResult(paymentOrder, currentStatus, "未生成微信商户订单号");
        }

        JSONObject queryResult = weChatPayService.queryOrderByOutTradeNo(paymentOrder.getWxOutTradeNo());
        String tradeState = queryResult.getString("trade_state");
        String tradeStateDesc = queryResult.getString("trade_state_desc");

        if ("SUCCESS".equals(tradeState)) {
            markOrderPaid(
                    paymentOrder,
                    queryResult.getString("transaction_id"),
                    parseWeChatPayTime(queryResult.getString("success_time"))
            );
            handleSuccessfulPayment(paymentOrder);
        } else if ("CLOSED".equals(tradeState) || "REVOKED".equals(tradeState)) {
            paymentOrder.setPaymentStatus(PaymentStatus.CLOSED.getCode());
            paymentOrderMapper.updateById(paymentOrder);
        } else if ("PAYERROR".equals(tradeState)) {
            paymentOrder.setPaymentStatus(PaymentStatus.FAILED.getCode());
            paymentOrderMapper.updateById(paymentOrder);
        }

        return buildPaymentStatusResult(paymentOrder, tradeState, tradeStateDesc);
    }

    /**
     * 2. 处理微信支付回调
     */
    @Transactional
    public void handlePaymentNotify(PaymentNotifyRequest notifyRequest) {
        try {
            log.info("收到支付回调通知: {}", JSON.toJSONString(notifyRequest));

            // 1. 验证回调签名
            boolean valid = weChatPayService.verifyNotification(
                    notifyRequest.getTimestamp(),
                    notifyRequest.getNonce(),
                    notifyRequest.getBody(),
                    notifyRequest.getSignature()
            );

            if (!valid) {
                log.error("支付回调签名验证失败");
                throw new BusinessException(400, "签名验证失败");
            }

            // 2. 解密回调数据
            String decryptedData = weChatPayService.decryptNotifyData(
                    notifyRequest.getResource().getCiphertext(),
                    notifyRequest.getResource().getNonce(),
                    notifyRequest.getResource().getAssociatedData()
            );

            JSONObject notifyData = JSON.parseObject(decryptedData);
            String outTradeNo = notifyData.getString("out_trade_no");
            String transactionId = notifyData.getString("transaction_id");
            String tradeState = notifyData.getString("trade_state");
            String successTime = notifyData.getString("success_time");

            // 3. 查询订单
            PaymentOrder paymentOrder = paymentOrderMapper.selectByOutTradeNo(outTradeNo);
            if (paymentOrder == null) {
                log.error("支付回调订单不存在: {}", outTradeNo);
                throw new BusinessException(404, "订单不存在");
            }

            // 4. 防止重复处理
            if (PaymentStatus.SUCCESS.getCode().equals(paymentOrder.getPaymentStatus())) {
                log.warn("订单已处理过，跳过重复回调: {}", outTradeNo);
                return;
            }

            // 5. 更新订单状态
            if ("SUCCESS".equals(tradeState)) {
                // 支付成功
                markOrderPaid(paymentOrder, transactionId, parseWeChatPayTime(successTime));

                // 6. 处理订单支付成功逻辑
                handleSuccessfulPayment(paymentOrder);

                log.info("支付成功处理完成: outTradeNo={}, transactionId={}", outTradeNo, transactionId);

            } else if ("REFUND".equals(tradeState)) {
                // 退款通知
                handleRefundNotify(paymentOrder, notifyData);
            } else {
                // 支付失败或其他状态
                paymentOrder.setPaymentStatus(PaymentStatus.FAILED.getCode());
                paymentOrderMapper.updateById(paymentOrder);
                log.warn("支付失败: outTradeNo={}, tradeState={}", outTradeNo, tradeState);
            }

        } catch (BusinessException e) {
            log.error("处理支付回调业务异常", e);
            throw e;
        } catch (Exception e) {
            log.error("处理支付回调异常", e);
            throw new BusinessException(500, "处理支付回调失败");
        }
    }

    /**
     * 3. 资金冻结（预授权）
     */
    @Transactional
    public FreezeResponse freezeDeposit(FreezeRequest request, String token) {
        try {
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

            // 3. 验证用户是否是契约参与方
            if (!contract.getInitiatorId().equals(currentUser.getId())
                    && !contract.getReceiverId().equals(currentUser.getId())) {
                throw new BusinessException(403, "无权操作此契约");
            }

            // 4. 检查是否已冻结过
            PaymentOrder existingFreeze = paymentOrderMapper.selectDepositFreezeOrder(
                    currentUser.getId(), request.getContractId());
            if (existingFreeze != null && PaymentStatus.SUCCESS.getCode().equals(existingFreeze.getPaymentStatus())) {
                throw new BusinessException(400, "押金已冻结");
            }

            // 5. 创建冻结订单
            PaymentOrder freezeOrder = createFreezeOrder(request, contract, currentUser);

            // 6. 调用微信支付分冻结接口
            Map<String, String> freezeResult = weChatPayService.freezeDeposit(freezeOrder, currentUser.getOpenid());

            // 7. 更新冻结信息
            if (freezeResult.containsKey("authorization_code")) {
                freezeOrder.setFreezeContractId(freezeResult.get("authorization_code"));
                freezeOrder.setFreezeTransactionId(freezeResult.get("transaction_id"));
                freezeOrder.setFreezeTime(LocalDateTime.now());
                paymentOrderMapper.updateById(freezeOrder);
            }

            // 8. 更新契约冻结状态
            updateContractFreezeStatus(contract, currentUser.getId());

            // 9. 构建响应
            FreezeResponse response = new FreezeResponse();
            response.setSuccess(true);
            response.setOrderId(freezeOrder.getId());
            response.setOrderNo(freezeOrder.getOrderNo());
            response.setContractId(contract.getId());
            response.setFreezeAmount(freezeOrder.getAmount());
            response.setAuthorizationCode(freezeOrder.getFreezeContractId());
            response.setMessage("资金冻结成功");

            log.info("资金冻结成功: userId={}, contractId={}, amount={}",
                    currentUser.getId(), contract.getId(), freezeOrder.getAmount());

            return response;

        } catch (BusinessException e) {
            log.error("资金冻结失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("资金冻结异常", e);
            throw new BusinessException(500, "资金冻结失败");
        }
    }

    /**
     * 4. 资金解冻
     */
    @Transactional
    public UnfreezeResponse unfreezeDeposit(UnfreezeRequest request) {
        try {
            // 1. 查询契约
            Contract contract = contractMapper.selectContractDetail(request.getContractId());
            if (contract == null) {
                throw new BusinessException(404, "契约不存在");
            }

            // 2. 查询双方的冻结订单
            PaymentOrder initiatorFreeze = paymentOrderMapper.selectDepositFreezeOrder(
                    contract.getInitiatorId(), contract.getId());
            PaymentOrder receiverFreeze = paymentOrderMapper.selectDepositFreezeOrder(
                    contract.getReceiverId(), contract.getId());

            List<PaymentOrder> freezeOrders = new ArrayList<>();
            if (initiatorFreeze != null) freezeOrders.add(initiatorFreeze);
            if (receiverFreeze != null) freezeOrders.add(receiverFreeze);

            if (freezeOrders.isEmpty()) {
                throw new BusinessException(400, "未找到冻结记录");
            }

            // 3. 批量解冻
            List<UnfreezeResponse.UnfreezeResult> results = new ArrayList<>();
            for (PaymentOrder freezeOrder : freezeOrders) {
                UnfreezeResponse.UnfreezeResult result = unfreezeSingleOrder(freezeOrder);
                results.add(result);
            }

            // 4. 更新契约解冻状态
            contractMapper.updateFreezeStatus(contract.getId(), "UNFROZEN");

            // 5. 构建响应
            UnfreezeResponse response = new UnfreezeResponse();
            response.setSuccess(true);
            response.setContractId(contract.getId());
            response.setResults(results);
            response.setMessage("资金解冻处理完成");

            log.info("资金解冻成功: contractId={}, 解冻{}笔订单", contract.getId(), results.size());

            return response;

        } catch (BusinessException e) {
            log.error("资金解冻失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("资金解冻异常", e);
            throw new BusinessException(500, "资金解冻失败");
        }
    }

    /**
     * 5. 扣除违约金
     */
    @Transactional
    public DeductResponse deductPenalty(DeductRequest request) {
        try {
            // 1. 查询契约
            Contract contract = contractMapper.selectContractDetail(request.getContractId());
            if (contract == null) {
                throw new BusinessException(404, "契约不存在");
            }

            if (!ContractStatus.DISPUTE.getCode().equals(contract.getStatus())) {
                throw new BusinessException(400, "契约非争议状态，无法扣款");
            }

            if (request.getViolatorUserId().equals(request.getVictimUserId())) {
                throw new BusinessException(400, "违约方与受害方不能为同一用户");
            }

            if (!request.getViolatorUserId().equals(contract.getInitiatorId())
                    && !request.getViolatorUserId().equals(contract.getReceiverId())) {
                throw new BusinessException(400, "违约者非契约参与方");
            }

            if (!request.getVictimUserId().equals(contract.getInitiatorId())
                    && !request.getVictimUserId().equals(contract.getReceiverId())) {
                throw new BusinessException(400, "受害者非契约参与方");
            }

            // 2. 查询违约者的冻结订单
            PaymentOrder violatorFreeze = paymentOrderMapper.selectDepositFreezeOrder(
                    request.getViolatorUserId(), contract.getId());

            if (violatorFreeze == null || !PaymentStatus.SUCCESS.getCode().equals(violatorFreeze.getPaymentStatus())) {
                throw new BusinessException(400, "违约者未冻结押金或冻结失败");
            }

            // 3. 检查冻结金额是否足够
            if (violatorFreeze.getAmount().compareTo(request.getDeductAmount()) < 0) {
                throw new BusinessException(400, "冻结金额不足");
            }

            // 4. 创建扣除订单
            PaymentOrder deductOrder = createDeductOrder(request, contract, violatorFreeze);

            // 5. 调用微信扣款接口
            boolean deductSuccess = weChatPayService.deductPenalty(violatorFreeze);

            if (deductSuccess) {
                // 6. 更新扣除订单状态
                deductOrder.setPaymentStatus(PaymentStatus.SUCCESS.getCode());
                deductOrder.setPayTime(LocalDateTime.now());
                paymentOrderMapper.updateById(deductOrder);

                // 7. 创建赔偿订单（给受害者）
                PaymentOrder compensationOrder = createCompensationOrder(request, contract);

                // 8. 更新契约违约状态
                contractMapper.markAsViolated(contract.getId());

                // 9. 更新用户信用分
                updateUserCreditAfterViolation(request.getViolatorUserId(), request.getVictimUserId());

                // 10. 构建响应
                DeductResponse response = new DeductResponse();
                response.setSuccess(true);
                response.setContractId(contract.getId());
                response.setDeductOrderId(deductOrder.getId());
                response.setCompensationOrderId(compensationOrder.getId());
                response.setDeductAmount(request.getDeductAmount());
                response.setCompensationAmount(compensationOrder.getAmount());
                response.setMessage("违约金扣除成功");

                log.info("违约金扣除成功: contractId={}, violator={}, victim={}, amount={}",
                        contract.getId(), request.getViolatorUserId(), request.getVictimUserId(),
                        request.getDeductAmount());

                return response;
            } else {
                throw new BusinessException(500, "微信扣款失败");
            }

        } catch (BusinessException e) {
            log.error("扣除违约金失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("扣除违约金异常", e);
            throw new BusinessException(500, "扣除违约金失败");
        }
    }

    // ============ 私有方法 ============

    /**
     * 验证支付请求
     */
    private void validatePaymentRequest(PaymentRequest request, User user) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "支付金额必须大于0");
        }

        // 验证订单类型
        resolveOrderType(request.getOrderType());

        String payMethod = StringUtils.hasText(request.getPayMethod())
                ? request.getPayMethod().trim().toUpperCase()
                : "WECHAT";
        request.setPayMethod(payMethod);
        if (!"WECHAT".equals(payMethod)) {
            throw new BusinessException(400, "当前仅支持微信支付");
        }
        if (!StringUtils.hasText(user.getOpenid())) {
            throw new BusinessException(400, "未获取到微信用户标识，请重新登录");
        }

        // 信用分充值无需契约ID
        if (OrderType.CREDIT_RECHARGE.getCode().equals(request.getOrderType())) {
            if (request.getAmount().compareTo(BigDecimal.ONE) < 0) {
                throw new BusinessException(400, "充值金额不能小于1元");
            }
            if (request.getAmount().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                throw new BusinessException(400, "充值金额必须为整数");
            }
            return;
        }

        if (!StringUtils.hasText(request.getContractId())) {
            throw new BusinessException(400, "契约ID不能为空");
        }

        // 如果是契约相关支付，验证契约
        if (StringUtils.hasText(request.getContractId())) {
            Contract contract = contractMapper.selectContractDetail(request.getContractId());
            if (contract == null) {
                throw new BusinessException(404, "契约不存在");
            }

            // 验证用户是否有权限
            if (!contract.getInitiatorId().equals(user.getId())
                    && !contract.getReceiverId().equals(user.getId())) {
                throw new BusinessException(403, "无权支付此契约");
            }

            // 检查是否已支付过
            List<PaymentOrder> existingOrders = paymentOrderMapper.selectByContractAndType(
                    request.getContractId(), request.getOrderType());

            for (PaymentOrder order : existingOrders) {
                if (order.getUserId().equals(user.getId())
                        && PaymentStatus.SUCCESS.getCode().equals(order.getPaymentStatus())) {
                    throw new BusinessException(400, "您已经支付过此费用");
                }
            }
        }
    }

    /**
     * 创建支付订单记录
     */
    private PaymentOrder createPaymentOrder(PaymentRequest request, User user) {
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(orderNoGenerator.generate("PAY"));
        order.setContractId(request.getContractId());
        order.setUserId(user.getId());
        order.setOrderType(request.getOrderType());
        order.setAmount(request.getAmount());
        order.setActualAmount(request.getAmount());
        order.setPaymentStatus(PaymentStatus.PENDING.getCode());
        order.setPaymentMethod(request.getPayMethod());
        order.setPayChannel(request.getPayMethod());

        if (OrderType.CREDIT_RECHARGE.getCode().equals(request.getOrderType())
                && !StringUtils.hasText(order.getContractId())) {
            order.setContractId("MOJIN_RECHARGE_" + user.getId());
        }

        // 计算手续费（根据不同订单类型）
        BigDecimal feeRate = calculateFeeRate(request.getOrderType());
        BigDecimal feeAmount = request.getAmount().multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        order.setFeeRate(feeRate);
        order.setFeeAmount(feeAmount);

        // 微信商户订单号
        order.setWxOutTradeNo(generateOutTradeNo());

        // 设置过期时间（默认30分钟）
        order.setExpireTime(LocalDateTime.now().plusMinutes(
                configService.getInt("payment.order_expire_minutes", 30)));

        // 设置回调URL
        if (StringUtils.hasText(request.getNotifyUrl())) {
            order.setNotifyUrl(request.getNotifyUrl());
        } else {
            order.setNotifyUrl(weChatPayConfig.getNotifyUrl());
        }

        Map<String, Object> businessData = new HashMap<>();
        if (request.getExtraData() != null && !request.getExtraData().isEmpty()) {
            businessData.putAll(request.getExtraData());
        }
        if (OrderType.CREDIT_RECHARGE.getCode().equals(request.getOrderType())) {
            businessData.put("rechargeMojin", resolveRechargeMojin(request.getAmount()));
        }

        // 存储扩展数据
        if (!businessData.isEmpty()) {
            order.setBusinessData(JSON.toJSONString(businessData));
        }

        paymentOrderMapper.insert(order);
        return order;
    }

    /**
     * 生成支付参数
     */
    private Map<String, String> generatePayParams(PaymentOrder order, User user) {
        if (OrderType.DEPOSIT_FREEZE.getCode().equals(order.getOrderType())) {
            // 资金冻结调用支付分接口
            return weChatPayService.freezeDeposit(order, user.getOpenid());
        } else {
            // 普通支付调用JSAPI接口
            return weChatPayService.unifiedOrder(order, user.getOpenid());
        }
    }

    /**
     * 处理支付成功逻辑
     */
    private void handleSuccessfulPayment(PaymentOrder order) {
        // 1. 根据订单类型处理不同逻辑
        switch (OrderType.fromCode(order.getOrderType())) {
            case SERVICE_FEE:
                // 服务费支付成功
                handleServiceFeePayment(order);
                break;

            case DEPOSIT_FREEZE:
                // 押金冻结成功
                handleDepositFreeze(order);
                break;

            case VIP_PAYMENT:
                // VIP支付成功
                handleVipPayment(order);
                break;

            case ARBITRATION_FEE:
                // 仲裁费支付成功
                handleArbitrationFee(order);
                break;

            case CREDIT_RECHARGE:
                // 摸金币充值成功
                handleMojinRecharge(order);
                break;

            default:
                log.warn("未知订单类型: {}", order.getOrderType());
        }

        // 2. 异步通知业务系统（如果配置了回调URL）
        if (StringUtils.hasText(order.getNotifyUrl())) {
            asyncNotifyBusinessSystem(order);
        }
    }

    /**
     * 创建冻结订单
     */
    private PaymentOrder createFreezeOrder(FreezeRequest request, Contract contract, User user) {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setContractId(request.getContractId());
        paymentRequest.setOrderType(OrderType.DEPOSIT_FREEZE.getCode());
        paymentRequest.setAmount(request.getFreezeAmount());

        return createPaymentOrder(paymentRequest, user);
    }

    /**
     * 更新契约冻结状态
     */
    private void handleMojinRecharge(PaymentOrder order) {
        User user = userMapper.selectById(order.getUserId());
        if (user == null) {
            log.warn("充值用户不存在: userId={}", order.getUserId());
            return;
        }

        BigDecimal rechargeMojin = null;
        if (StringUtils.hasText(order.getBusinessData())) {
            try {
                rechargeMojin = JSON.parseObject(order.getBusinessData()).getBigDecimal("rechargeMojin");
            } catch (Exception e) {
                log.warn("解析充值档位扩展数据失败: orderNo={}", order.getOrderNo(), e);
            }
        }

        BigDecimal addMojin = rechargeMojin != null
                ? rechargeMojin
                : resolveRechargeMojin(order.getAmount());
        if (addMojin.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("充值金额不足以增加摸金币: orderNo={}", order.getOrderNo());
            return;
        }

        userMapper.incrementMojinBalance(user.getId(), addMojin);
        User refreshedUser = userMapper.selectById(user.getId());
        if (refreshedUser == null) {
            log.warn("充值后用户不存在: userId={}", user.getId());
            return;
        }

        BigDecimal afterBalance = refreshedUser.getMojinBalance() == null ? BigDecimal.ZERO : refreshedUser.getMojinBalance();
        BigDecimal beforeBalance = afterBalance.subtract(addMojin);

        MojinLedger ledger = new MojinLedger();
        ledger.setUserId(user.getId());
        ledger.setChangeAmount(addMojin);
        ledger.setBeforeBalance(beforeBalance);
        ledger.setAfterBalance(afterBalance);
        ledger.setChangeType("RECHARGE");
        ledger.setRelatedId(order.getOrderNo());
        ledger.setRelatedType("PAYMENT_ORDER");
        ledger.setDescription("摸金币充值");
        mojinLedgerMapper.insert(ledger);

        log.info("摸金币充值成功: userId={}, addMojin={}, afterBalance={}",
                user.getId(), addMojin, afterBalance);
    }

    private void markOrderPaid(PaymentOrder paymentOrder, String transactionId, LocalDateTime payTime) {
        paymentOrder.setPaymentStatus(PaymentStatus.SUCCESS.getCode());
        paymentOrder.setWxTransactionId(transactionId);
        paymentOrder.setPayTime(payTime == null ? LocalDateTime.now() : payTime);
        paymentOrder.setCallbackStatus("SUCCESS");
        paymentOrder.setLastCallbackTime(LocalDateTime.now());
        paymentOrderMapper.updateById(paymentOrder);
    }

    private LocalDateTime parseWeChatPayTime(String successTime) {
        if (!StringUtils.hasText(successTime)) {
            return LocalDateTime.now();
        }
        try {
            return OffsetDateTime.parse(successTime).toLocalDateTime();
        } catch (Exception e) {
            log.warn("解析微信支付时间失败，使用当前时间兜底: {}", successTime, e);
            return LocalDateTime.now();
        }
    }

    private Map<String, Object> buildPaymentStatusResult(PaymentOrder paymentOrder, String tradeState, String tradeStateDesc) {
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", paymentOrder.getOrderNo());
        result.put("orderType", paymentOrder.getOrderType());
        result.put("amount", paymentOrder.getAmount());
        result.put("paymentStatus", paymentOrder.getPaymentStatus());
        result.put("tradeState", tradeState);
        result.put("tradeStateDesc", tradeStateDesc);
        result.put("transactionId", paymentOrder.getWxTransactionId());
        result.put("payTime", paymentOrder.getPayTime());
        result.put("paid", PaymentStatus.SUCCESS.getCode().equals(paymentOrder.getPaymentStatus()));
        return result;
    }

    private OrderType resolveOrderType(String orderTypeCode) {
        for (OrderType orderType : OrderType.values()) {
            if (orderType.getCode().equals(orderTypeCode)) {
                return orderType;
            }
        }
        throw new BusinessException(400, "不支持的订单类型");
    }

    private BigDecimal resolveRechargeMojin(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(RMB_TO_MOJIN_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private void updateContractFreezeStatus(Contract contract, Long userId) {
        String currentFreezeStatus = contract.getFreezeStatus();
        String newFreezeStatus;

        if (currentFreezeStatus == null || "NONE".equals(currentFreezeStatus)) {
            // 第一笔冻结
            newFreezeStatus = contract.getInitiatorId().equals(userId) ?
                    "INITIATOR_FROZEN" : "RECEIVER_FROZEN";
        } else if ("INITIATOR_FROZEN".equals(currentFreezeStatus) && contract.getReceiverId().equals(userId)) {
            // 发起人已冻结，接收人现在冻结
            newFreezeStatus = "BOTH_FROZEN";
        } else if ("RECEIVER_FROZEN".equals(currentFreezeStatus) && contract.getInitiatorId().equals(userId)) {
            // 接收人已冻结，发起人现在冻结
            newFreezeStatus = "BOTH_FROZEN";
        } else {
            // 已经冻结过
            return;
        }

        contractMapper.updateFreezeStatus(contract.getId(), newFreezeStatus);
    }

    /**
     * 解冻单个订单
     */
    private UnfreezeResponse.UnfreezeResult unfreezeSingleOrder(PaymentOrder freezeOrder) {
        UnfreezeResponse.UnfreezeResult result = new UnfreezeResponse.UnfreezeResult();
        result.setUserId(freezeOrder.getUserId());
        result.setFreezeOrderId(freezeOrder.getId());
        result.setFreezeAmount(freezeOrder.getAmount());

        try {
            // 1. 创建解冻订单
            PaymentOrder unfreezeOrder = createUnfreezeOrder(freezeOrder);

            // 2. 调用微信解冻接口
            boolean success = weChatPayService.unfreezeDeposit(freezeOrder);

            if (success) {
                // 3. 更新解冻订单状态
                unfreezeOrder.setPaymentStatus(PaymentStatus.SUCCESS.getCode());
                unfreezeOrder.setUnfreezeTime(LocalDateTime.now());
                paymentOrderMapper.updateById(unfreezeOrder);

                // 4. 更新冻结订单的解冻信息
                freezeOrder.setUnfreezeTransactionId(unfreezeOrder.getOrderNo());
                freezeOrder.setUnfreezeTime(LocalDateTime.now());
                paymentOrderMapper.updateById(freezeOrder);

                result.setSuccess(true);
                result.setUnfreezeOrderId(unfreezeOrder.getId());
                result.setMessage("解冻成功");

                log.info("订单解冻成功: userId={}, amount={}",
                        freezeOrder.getUserId(), freezeOrder.getAmount());
            } else {
                result.setSuccess(false);
                result.setMessage("微信解冻接口调用失败");
                log.error("解冻失败: userId={}, orderId={}",
                        freezeOrder.getUserId(), freezeOrder.getId());
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("解冻异常: " + e.getMessage());
            log.error("解冻异常", e);
        }

        return result;
    }

    /**
     * 创建解冻订单
     */
    private PaymentOrder createUnfreezeOrder(PaymentOrder freezeOrder) {
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(orderNoGenerator.generate("UNFREEZE"));
        order.setContractId(freezeOrder.getContractId());
        order.setUserId(freezeOrder.getUserId());
        order.setOrderType(OrderType.DEPOSIT_UNFREEZE.getCode());
        order.setAmount(freezeOrder.getAmount());
        order.setActualAmount(freezeOrder.getAmount());
        order.setPaymentStatus(PaymentStatus.PENDING.getCode());
        order.setFreezeContractId(freezeOrder.getFreezeContractId());

        paymentOrderMapper.insert(order);
        return order;
    }

    /**
     * 创建扣除订单
     */
    private PaymentOrder createDeductOrder(DeductRequest request, Contract contract, PaymentOrder freezeOrder) {
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(orderNoGenerator.generate("DEDUCT"));
        order.setContractId(contract.getId());
        order.setUserId(request.getViolatorUserId());
        order.setOrderType(OrderType.DEPOSIT_DEDUCT.getCode());
        order.setAmount(request.getDeductAmount());
        order.setActualAmount(request.getDeductAmount());
        order.setPaymentStatus(PaymentStatus.PENDING.getCode());
        order.setFreezeContractId(freezeOrder.getFreezeContractId());

        // 存储违约信息
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("violatorUserId", request.getViolatorUserId());
        businessData.put("victimUserId", request.getVictimUserId());
        businessData.put("reason", request.getReason());
        businessData.put("evidence", request.getEvidence());
        order.setBusinessData(JSON.toJSONString(businessData));

        paymentOrderMapper.insert(order);
        return order;
    }

    /**
     * 创建赔偿订单
     */
    private PaymentOrder createCompensationOrder(DeductRequest request, Contract contract) {
        // 计算实际赔偿金额（扣除1%手续费）
        BigDecimal feeRate = configService.getDecimal("payment.penalty_fee_rate", new BigDecimal("0.01"));
        BigDecimal compensationAmount = request.getDeductAmount()
                .subtract(request.getDeductAmount().multiply(feeRate))
                .setScale(2, RoundingMode.HALF_UP);

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(orderNoGenerator.generate("COMP"));
        order.setContractId(contract.getId());
        order.setUserId(request.getVictimUserId());
        order.setOrderType(OrderType.COMPENSATION.getCode());
        order.setAmount(compensationAmount);
        order.setActualAmount(compensationAmount);
        order.setPaymentStatus(PaymentStatus.SUCCESS.getCode());
        order.setPayTime(LocalDateTime.now());

        paymentOrderMapper.insert(order);
        return order;
    }

    /**
     * 计算手续费率
     */
    private BigDecimal calculateFeeRate(String orderType) {
        switch (OrderType.fromCode(orderType)) {
            case SERVICE_FEE:
                return BigDecimal.ZERO; // 服务费不收手续费
            case DEPOSIT_FREEZE:
                return BigDecimal.ZERO; // 冻结不收手续费
            case VIP_PAYMENT:
                return new BigDecimal("0.006"); // 0.6%
            case ARBITRATION_FEE:
                return new BigDecimal("0.006"); // 0.6%
            case CREDIT_RECHARGE:
                return BigDecimal.ZERO; // 充值不收手续费
            default:
                return new BigDecimal("0.006"); // 默认0.6%
        }
    }

    /**
     * 生成微信商户订单号
     */
    private String generateOutTradeNo() {
        return "WX" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    /**
     * 处理退款通知
     */
    private void handleRefundNotify(PaymentOrder order, JSONObject notifyData) {
        // 实现退款处理逻辑
        log.info("处理退款通知: orderNo={}", order.getOrderNo());
    }

    /**
     * 处理服务费支付成功
     */
    private void handleServiceFeePayment(PaymentOrder order) {
        // 检查契约是否所有费用都已支付，如果都已支付则开始契约
        checkAndStartContract(order.getContractId());
    }

    /**
     * 处理押金冻结成功
     */
    private void handleDepositFreeze(PaymentOrder order) {
        // 检查契约是否所有费用都已支付，如果都已支付则开始契约
        checkAndStartContract(order.getContractId());
    }

    /**
     * 处理VIP支付成功
     */
    private void handleVipPayment(PaymentOrder order) {
        // 更新用户VIP状态
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            user.setIsVip(true);
            user.setVipStartTime(LocalDateTime.now());
            user.setVipExpireTime(LocalDateTime.now().plusMonths(1)); // 默认一个月
            userMapper.updateById(user);
            log.info("用户VIP状态更新: userId={}", user.getId());
        }
    }

    /**
     * 处理仲裁费支付成功
     */
    private void handleArbitrationFee(PaymentOrder order) {
        // 更新仲裁状态为加急处理
        // 这里需要调用仲裁服务
        log.info("仲裁费支付成功，标记为加急处理: orderId={}", order.getId());
    }

    /**
     * 检查并开始契约
     */
    private void checkAndStartContract(String contractId) {
        // 检查契约是否所有费用都已支付
        boolean allPaid = checkContractPaymentStatus(contractId);

        if (allPaid) {
            // 更新契约状态为已支付
            contractMapper.updatePaymentStatus(contractId, "FULL");
            contractMapper.updateContractStatus(contractId, ContractStatus.PAID.getCode());
            log.info("契约所有费用支付完成，准备开始: contractId={}", contractId);
        }
    }

    /**
     * 检查契约支付状态
     */
    private boolean checkContractPaymentStatus(String contractId) {
        // 查询契约所有支付订单
        List<PaymentOrder> orders = paymentOrderMapper.selectByContractId(contractId);

        // 检查服务费和押金是否都已支付成功
        boolean serviceFeePaid = orders.stream()
                .filter(o -> OrderType.SERVICE_FEE.getCode().equals(o.getOrderType()))
                .allMatch(o -> PaymentStatus.SUCCESS.getCode().equals(o.getPaymentStatus()));

        boolean depositPaid = orders.stream()
                .filter(o -> OrderType.DEPOSIT_FREEZE.getCode().equals(o.getOrderType()))
                .allMatch(o -> PaymentStatus.SUCCESS.getCode().equals(o.getPaymentStatus()));

        return serviceFeePaid && depositPaid;
    }

    /**
     * 异步通知业务系统
     */
    private void asyncNotifyBusinessSystem(PaymentOrder order) {
        // 实现异步通知逻辑
        // 可以使用消息队列或线程池
        log.info("异步通知业务系统: orderId={}, notifyUrl={}", order.getId(), order.getNotifyUrl());
    }

    /**
     * 更新用户信用分（违约后）
     */
    private void updateUserCreditAfterViolation(Long violatorUserId, Long victimUserId) {
        // 违约者扣分
        int violationDeduct = configService.getInt("credit.violation_deduct", 50);
        User violator = userMapper.selectById(violatorUserId);
        if (violator != null) {
            int newScore = Math.max(0, violator.getCreditScore() - violationDeduct);
            violator.setCreditScore(newScore);
            violator.setViolationCount(violator.getViolationCount() + 1);
            userMapper.updateById(violator);

            // 记录信用历史
            log.info("违约者信用分更新: userId={}, 扣除{}分", violatorUserId, violationDeduct);
        }

        // 受害者可以适当加分（可选）
        int compensationAdd = configService.getInt("credit.compensation_add", 5);
        User victim = userMapper.selectById(victimUserId);
        if (victim != null) {
            int newScore = Math.min(100, victim.getCreditScore() + compensationAdd);
            victim.setCreditScore(newScore);
            userMapper.updateById(victim);

            log.info("受害者信用分更新: userId={}, 增加{}分", victimUserId, compensationAdd);
        }
    }

    /**
     * 退款处理（如果已支付）
     * 用于契约取消或超时时，对已支付的订单进行退款
     */
    @Transactional
    public void refundIfPaid(String contractId) {
        try {
            log.info("开始处理契约退款: contractId={}", contractId);

            // 1. 查询契约相关的所有支付订单
            List<PaymentOrder> paymentOrders = paymentOrderMapper.selectByContractId(contractId);

            if (paymentOrders == null || paymentOrders.isEmpty()) {
                log.info("契约无支付订单，无需退款: contractId={}", contractId);
                return;
            }

            // 2. 过滤已支付且未退款的订单
            List<PaymentOrder> ordersToRefund = paymentOrders.stream()
                    .filter(order -> PaymentStatus.SUCCESS.getCode().equals(order.getPaymentStatus()))
                    .filter(order -> PaymentStatus.REFUNDED.getCode().equals(order.getRefundStatus())
                            || "NONE".equals(order.getRefundStatus()))
                    .collect(Collectors.toList());

            if (ordersToRefund.isEmpty()) {
                log.info("没有需要退款的订单: contractId={}", contractId);
                return;
            }

            log.info("发现{}笔需要退款的订单: contractId={}", ordersToRefund.size(), contractId);

            // 3. 批量处理退款
            int successCount = 0;
            int failedCount = 0;

            for (PaymentOrder order : ordersToRefund) {
                try {
                    boolean refundSuccess = processRefund(order);

                    if (refundSuccess) {
                        successCount++;

                        // 更新订单退款状态
                        order.setRefundStatus(PaymentStatus.REFUNDED.getCode());
                        order.setRefundTime(LocalDateTime.now());
                        paymentOrderMapper.updateById(order);

                        // 记录退款日志
                        logRefundOperation(order, "契约取消自动退款");

                        log.info("订单退款成功: orderNo={}, userId={}, amount={}",
                                order.getOrderNo(), order.getUserId(), order.getAmount());
                    } else {
                        failedCount++;
                        log.error("订单退款失败: orderNo={}", order.getOrderNo());

                        // 标记为退款失败
                        order.setRefundStatus(PaymentStatus.REFUND_FAILED.getCode());
                        paymentOrderMapper.updateById(order);
                    }

                } catch (Exception e) {
                    failedCount++;
                    log.error("处理订单退款异常: orderId={}", order.getId(), e);

                    // 标记为退款失败
                    order.setRefundStatus(PaymentStatus.REFUND_FAILED.getCode());
                    paymentOrderMapper.updateById(order);
                }
            }

            // 4. 根据订单类型更新相关状态
            updateRelatedStatusAfterRefund(contractId, ordersToRefund);

            log.info("契约退款处理完成: contractId={}, 成功{}笔, 失败{}笔",
                    contractId, successCount, failedCount);

        } catch (Exception e) {
            log.error("处理契约退款异常: contractId={}", contractId, e);
            throw new BusinessException(500, "退款处理失败");
        }
    }

    /**
     * 处理单个订单退款
     */
    private boolean processRefund(PaymentOrder order) {
        try {
            // 1. 创建退款订单记录
            PaymentOrder refundOrder = createRefundOrder(order);

            // 2. 根据订单类型调用不同的退款接口
            boolean refundSuccess = false;

            if (OrderType.DEPOSIT_FREEZE.getCode().equals(order.getOrderType())) {
                // 押金冻结订单：直接解冻，无需调用退款API
                refundSuccess = true;
                log.info("押金冻结订单直接解冻: orderNo={}", order.getOrderNo());

            } else if (OrderType.SERVICE_FEE.getCode().equals(order.getOrderType())) {
                // 服务费订单：调用微信退款API
                refundSuccess = weChatPayService.refundPayment(order, refundOrder);

            } else if (OrderType.VIP_PAYMENT.getCode().equals(order.getOrderType())) {
                // VIP支付订单：调用微信退款API
                refundSuccess = weChatPayService.refundPayment(order, refundOrder);

            } else if (OrderType.ARBITRATION_FEE.getCode().equals(order.getOrderType())) {
                // 仲裁费订单：调用微信退款API
                refundSuccess = weChatPayService.refundPayment(order, refundOrder);

            } else {
                // 其他类型订单不支持退款
                log.warn("不支持退款的订单类型: orderType={}, orderNo={}",
                        order.getOrderType(), order.getOrderNo());
                return false;
            }

            // 3. 更新退款订单状态
            if (refundSuccess) {
                refundOrder.setPaymentStatus(PaymentStatus.SUCCESS.getCode());
                refundOrder.setPayTime(LocalDateTime.now());
                paymentOrderMapper.updateById(refundOrder);

                // 关联原订单和退款订单
                order.setRefundStatus(PaymentStatus.REFUNDED.getCode());
                order.setRefundTime(LocalDateTime.now());
                paymentOrderMapper.updateById(order);

                // 记录退款流水
                recordRefundTransaction(order, refundOrder);
            } else {
                refundOrder.setPaymentStatus(PaymentStatus.FAILED.getCode());
                paymentOrderMapper.updateById(refundOrder);
            }

            return refundSuccess;

        } catch (Exception e) {
            log.error("处理退款异常: orderId={}", order.getId(), e);
            return false;
        }
    }

    /**
     * 创建退款订单
     */
    private PaymentOrder createRefundOrder(PaymentOrder originalOrder) {
        PaymentOrder refundOrder = new PaymentOrder();
        refundOrder.setOrderNo(orderNoGenerator.generate("REFUND"));
        refundOrder.setContractId(originalOrder.getContractId());
        refundOrder.setUserId(originalOrder.getUserId());
        refundOrder.setOrderType(OrderType.REFUND.getCode());
        refundOrder.setAmount(originalOrder.getAmount());
        refundOrder.setActualAmount(originalOrder.getAmount());
        refundOrder.setPaymentStatus(PaymentStatus.PENDING.getCode());
        refundOrder.setWxOutTradeNo("REFUND_" + originalOrder.getWxOutTradeNo());

        // 关联原订单信息
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("originalOrderId", originalOrder.getId());
        businessData.put("originalOrderNo", originalOrder.getOrderNo());
        businessData.put("refundReason", "契约取消自动退款");
        refundOrder.setBusinessData(JSON.toJSONString(businessData));

        paymentOrderMapper.insert(refundOrder);
        return refundOrder;
    }

    /**
     * 更新退款后的相关状态
     */
    private void updateRelatedStatusAfterRefund(String contractId, List<PaymentOrder> refundedOrders) {
        try {
            // 1. 检查是否所有费用都已退款
            List<PaymentOrder> allOrders = paymentOrderMapper.selectByContractId(contractId);

            boolean allRefunded = allOrders.stream()
                    .filter(order -> PaymentStatus.SUCCESS.getCode().equals(order.getPaymentStatus()))
                    .allMatch(order -> PaymentStatus.REFUNDED.getCode().equals(order.getRefundStatus()));

            if (allRefunded) {
                // 2. 更新契约退款状态
                contractMapper.updateRefundStatus(contractId, "FULL");
                log.info("契约所有费用已全额退款: contractId={}", contractId);
            } else {
                // 部分退款
                contractMapper.updateRefundStatus(contractId, "PARTIAL");
                log.info("契约部分费用已退款: contractId={}", contractId);
            }

            // 3. 对于押金冻结订单，需要更新冻结状态
            refundedOrders.stream()
                    .filter(order -> OrderType.DEPOSIT_FREEZE.getCode().equals(order.getOrderType()))
                    .forEach(order -> {
                        // 如果押金已解冻，更新冻结状态
                        if (order.getUnfreezeTransactionId() != null) {
                            contractMapper.updateFreezeStatus(contractId, "REFUNDED");
                        }
                    });

            // 4. 如果是VIP订单退款，需要更新用户VIP状态
            refundedOrders.stream()
                    .filter(order -> OrderType.VIP_PAYMENT.getCode().equals(order.getOrderType()))
                    .forEach(order -> {
                        updateUserVipStatusAfterRefund(order.getUserId());
                    });

        } catch (Exception e) {
            log.error("更新退款后状态异常: contractId={}", contractId, e);
        }
    }

    /**
     * 更新用户VIP状态（退款后）
     */
    private void updateUserVipStatusAfterRefund(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user != null && Boolean.TRUE.equals(user.getIsVip())) {
                // 检查用户是否还有其他有效的VIP订单
                List<PaymentOrder> vipOrders = paymentOrderMapper.selectUserOrders(
                        userId, OrderType.VIP_PAYMENT.getCode(), PaymentStatus.SUCCESS.getCode(), 0, 10);

                boolean hasValidVipOrder = vipOrders.stream()
                        .anyMatch(order -> !PaymentStatus.REFUNDED.getCode().equals(order.getRefundStatus()));

                if (!hasValidVipOrder) {
                    // 没有其他有效VIP订单，取消VIP状态
                    user.setIsVip(false);
                    user.setVipExpireTime(LocalDateTime.now());
                    userMapper.updateById(user);
                    log.info("用户VIP状态已取消（退款后）: userId={}", userId);
                }
            }
        } catch (Exception e) {
            log.error("更新用户VIP状态异常: userId={}", userId, e);
        }
    }

    /**
     * 记录退款操作日志
     */
    private void logRefundOperation(PaymentOrder order, String reason) {
        try {
            // 这里可以记录到专门的退款日志表或操作日志表
            log.info("退款操作记录: orderNo={}, userId={}, amount={}, reason={}",
                    order.getOrderNo(), order.getUserId(), order.getAmount(), reason);

            // 如果需要更详细的记录，可以创建一个RefundLog实体类
            // refundLogService.save(new RefundLog(order, reason));

        } catch (Exception e) {
            log.error("记录退款日志异常", e);
        }
    }

    /**
     * 记录退款流水
     */
    private void recordRefundTransaction(PaymentOrder originalOrder, PaymentOrder refundOrder) {
        try {
            // 这里可以记录到财务流水表
            log.info("退款流水记录: originalOrderNo={}, refundOrderNo={}, amount={}",
                    originalOrder.getOrderNo(), refundOrder.getOrderNo(), refundOrder.getAmount());

            // 如果需要，可以创建一个TransactionRecord实体类
            // transactionService.recordRefund(originalOrder, refundOrder);

        } catch (Exception e) {
            log.error("记录退款流水异常", e);
        }
    }
}
