package net.coding.template.service;

import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.enums.ContractPhase;
import net.coding.template.entity.po.Contract;
import net.coding.template.entity.po.ContractConfirm;
import net.coding.template.entity.po.User;
import net.coding.template.entity.enums.ConfirmStatus;
import net.coding.template.entity.enums.ContractStatus;
import net.coding.template.entity.request.ContractConfirmRequest;
import net.coding.template.entity.request.ContractCreateRequest;
import net.coding.template.entity.dto.ContractDetailDTO;
import net.coding.template.entity.request.ContractListRequest;
import net.coding.template.entity.request.UnfreezeRequest;
import net.coding.template.entity.response.ContractConfirmResponse;
import net.coding.template.entity.response.ContractCreateResponse;
import net.coding.template.entity.response.ContractListResponse;
import net.coding.template.exception.BusinessException;
import net.coding.template.mapper.ContractConfirmMapper;
import net.coding.template.mapper.ContractMapper;
import net.coding.template.mapper.PaymentOrderMapper;
import net.coding.template.mapper.UserMapper;
import net.coding.template.util.ContractNoGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContractService {

    @Resource
    private ContractMapper contractMapper;

    @Resource
    private ContractConfirmMapper contractConfirmMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private net.coding.template.mapper.UserGameAccountMapper userGameAccountMapper;

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private UserService userService;

    @Resource
    private PaymentService paymentService;

    @Resource
    private ContractNoGenerator contractNoGenerator;


    @Resource
    private SystemConfigService configService;

    @Resource
    private RedisService redisService;

    /**
     * 创建契约
     */
    @Transactional
    public ContractCreateResponse createContract(ContractCreateRequest request, String token) {
        // 1. 获取当前用户
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 2. 验证用户是否有进行中的契约
        int activeCount = contractMapper.countActiveContracts(currentUser.getId());
        if (activeCount >= 3) { // 限制同时进行3个契约
            throw new BusinessException(400, "您已有进行中的契约，请先完成");
        }

        // 3. 创建契约记录（接收人由接单时绑定）
        Contract contract = new Contract();
        contract.setId(generateContractId());
        contract.setInitiatorId(currentUser.getId());
        contract.setReceiverId(null);
        contract.setDepositAmount(request.getDepositAmount());
        contract.setGameType(request.getGameType());

        // 计算服务费（如果是VIP则免费）
        BigDecimal serviceFee = currentUser.getIsVip() ? BigDecimal.ZERO :
                calculateServiceFee(request.getDepositAmount());
        contract.setServiceFeeAmount(serviceFee);

        contract.setTitle(request.getTitle());
        contract.setGuaranteeItem(null);
        contract.setSuccessCondition(request.getSuccessCondition());
        contract.setFailureCondition(request.getFailureCondition());
        contract.setMinCredit(request.getMinCredit());
        contract.setRequirements(request.getRequirements());
        contract.setDescription(request.getDescription());
        contract.setCoverUrl(request.getCoverUrl());
        contract.setStatus(ContractStatus.PENDING.getCode());
        contract.setPhase(ContractPhase.PREPARE.getCode());
        contract.setPaymentStatus("UNPAID");
        contract.setFreezeStatus("NONE");

        boolean inserted = false;
        int attempts = 0;
        while (!inserted && attempts < 5) {
            attempts++;
            contract.setContractNo(contractNoGenerator.generate());
            try {
                int rows = contractMapper.insert(contract);
                inserted = rows > 0;
            } catch (org.springframework.dao.DuplicateKeyException ex) {
                log.warn("契约号重复，重试生成: {}", contract.getContractNo());
            }
        }
        if (!inserted) {
            throw new BusinessException(500, "创建契约失败");
        }

        // 7. 创建确认记录
        createConfirmRecords(contract);

        // 8. 更新用户统计
        userMapper.incrementTotalContracts(currentUser.getId());

        // 9. 返回结果
        ContractCreateResponse response = new ContractCreateResponse();
        response.setContractId(contract.getId());
        response.setContractNo(contract.getContractNo());
        response.setStatus(contract.getStatus());
        response.setTitle(contract.getTitle());
        response.setGameType(contract.getGameType());
        response.setDepositAmount(contract.getDepositAmount());
        response.setServiceFeeAmount(contract.getServiceFeeAmount());
        response.setReceiverInfo(null);
        response.setCreateTime(contract.getCreateTime());

        log.info("契约创建成功: 发起人={}, 契约号: {}",
                currentUser.getId(), contract.getContractNo());

        return response;
    }

    /**
     * 获取契约列表
     */
    public ContractListResponse getContractList(ContractListRequest request, String token) {
        // 1. 获取当前用户
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 2. 查询契约列表
        int offset = (request.getPage() - 1) * request.getSize();
        List<Contract> contracts = contractMapper.selectUserContracts(
                currentUser.getId(),
                request.getScope(),
                request.getStatus(),
                offset,
                request.getSize()
        );

        // 3. 统计总数
        int total = contractMapper.countUserContracts(currentUser.getId(), request.getScope(), request.getStatus());

        // 4. 转换为DTO
        List<ContractListResponse.ContractItem> items = contracts.stream()
                .map(this::convertToContractItem)
                .collect(Collectors.toList());

        // 5. 构建响应
        ContractListResponse response = new ContractListResponse();
        response.setTotal(total);
        response.setPage(request.getPage());
        response.setSize(request.getSize());
        response.setTotalPages((int) Math.ceil((double) total / request.getSize()));
        response.setContracts(items);

        return response;
    }

    /**
     * 契约大厅列表（待接单）
     */
    public ContractListResponse getHallList(ContractListRequest request) {
        // 参数校验
        if (request.getPage() == null || request.getPage() < 1) {
            request.setPage(1);
        }
        if (request.getSize() == null || request.getSize() < 1 || request.getSize() > 100) {
            request.setSize(20);
        }

        int offset = (request.getPage() - 1) * request.getSize();
        List<Contract> contracts = contractMapper.selectHallContracts(
                request.getKeyword(),
                request.getContractNo(),
                request.getGameType(),
                offset,
                request.getSize()
        );
        int total = contractMapper.countHallContracts(
                request.getKeyword(),
                request.getContractNo(),
                request.getGameType()
        );

        List<ContractListResponse.ContractItem> items = contracts.stream()
                .map(this::convertToContractItem)
                .collect(Collectors.toList());

        ContractListResponse response = new ContractListResponse();
        response.setTotal(total);
        response.setPage(request.getPage());
        response.setSize(request.getSize());
        response.setTotalPages((int) Math.ceil((double) total / request.getSize()));
        response.setContracts(items);

        return response;
    }

    /**
     * 接单
     */
    @Transactional
    public void acceptContract(net.coding.template.entity.request.ContractAcceptRequest request, String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        Contract contract = contractMapper.selectContractDetail(request.getContractId());
        if (contract == null) {
            throw new BusinessException(404, "契约不存在");
        }
        if (contract.getReceiverId() != null) {
            throw new BusinessException(400, "该契约已被接单");
        }
        if (contract.getInitiatorId().equals(currentUser.getId())) {
            throw new BusinessException(400, "不能接自己的契约");
        }

        int rows = contractMapper.acceptContract(contract.getId(), currentUser.getId());
        if (rows == 0) {
            throw new BusinessException(400, "接单失败");
        }

        // 接单后创建接收方确认记录
        ContractConfirm receiverConfirm = new ContractConfirm();
        receiverConfirm.setContractId(contract.getId());
        receiverConfirm.setUserId(currentUser.getId());
        receiverConfirm.setUserRole("RECEIVER");
        receiverConfirm.setConfirmStatus(ConfirmStatus.PENDING.getCode());
        contractConfirmMapper.insert(receiverConfirm);

        // 接单后状态进入已支付待开始
        contractMapper.updateContractStatus(contract.getId(), ContractStatus.PAID.getCode());
    }

    /**
     * 获取契约详情
     */
    public ContractDetailDTO getContractDetail(String contractId, String token) {
        // 1. 获取当前用户
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 2. 查询契约详情
        Contract contract = contractMapper.selectContractDetail(contractId);
        if (contract == null) {
            throw new BusinessException(404, "契约不存在");
        }

        // 3. 验证用户是否有权限查看
        boolean isInitiator = contract.getInitiatorId() != null
                && contract.getInitiatorId().equals(currentUser.getId());
        boolean isReceiver = contract.getReceiverId() != null
                && contract.getReceiverId().equals(currentUser.getId());
        boolean isHallVisible = contract.getReceiverId() == null
                && net.coding.template.entity.enums.ContractStatus.PENDING.getCode().equals(contract.getStatus());
        if (!isInitiator && !isReceiver && !isHallVisible) {
            throw new BusinessException(403, "无权查看此契约");
        }

        // 4. 查询双方用户信息
        User initiator = userMapper.selectById(contract.getInitiatorId());
        User receiver = userMapper.selectById(contract.getReceiverId());

        // 5. 查询确认状态
        List<ContractConfirm> confirms = contractConfirmMapper.selectContractConfirms(contractId);

        // 6. 构建响应
        ContractDetailDTO dto = new ContractDetailDTO();
        dto.setContractId(contract.getId());
        dto.setContractNo(contract.getContractNo());
        dto.setStatus(contract.getStatus());
        dto.setPhase(contract.getPhase());
        dto.setTitle(contract.getTitle());
        dto.setGameType(contract.getGameType());

        // 用户信息
        dto.setInitiator(buildUserInfo(initiator));
        dto.setReceiver(buildUserInfo(receiver));

        // 契约条款
        dto.setDepositAmount(contract.getDepositAmount());
        dto.setServiceFeeAmount(contract.getServiceFeeAmount());
        dto.setGuaranteeItem(contract.getGuaranteeItem());
        dto.setMinCredit(contract.getMinCredit());
        dto.setRequirements(contract.getRequirements());
        dto.setDescription(contract.getDescription());
        dto.setCoverUrl(contract.getCoverUrl());
        dto.setSuccessCondition(contract.getSuccessCondition());
        dto.setFailureCondition(contract.getFailureCondition());

        // 时间信息
        dto.setCreateTime(contract.getCreateTime());
        dto.setStartTime(contract.getStartTime());
        dto.setEndTime(contract.getEndTime());
        dto.setCompleteTime(contract.getCompleteTime());
        dto.setUpdateTime(contract.getUpdateTime());
        if (ContractStatus.PAID.getCode().equals(contract.getStatus())
                && contract.getUpdateTime() != null) {
            dto.setAcceptExpireTime(contract.getUpdateTime().plusMinutes(30));
        }

        // 确认状态
        dto.setConfirmStatus(buildConfirmStatus(confirms));

        // 操作权限
        dto.setCanConfirm(canConfirmContract(contract, currentUser, confirms));
        dto.setCanCancel(canCancelContract(contract, currentUser));
        dto.setCanStart(canStartContract(contract, currentUser));
        dto.setCanComplete(canCompleteContract(contract, currentUser));
        dto.setCanDispute(canDisputeContract(contract, currentUser));

        // 签订/完成按钮权限
        dto.setRole(isInitiator ? "INITIATOR" : "RECEIVER");
        dto.setDepositRequired(contract.getDepositAmount() != null
                && contract.getDepositAmount().compareTo(java.math.BigDecimal.ZERO) > 0);
        dto.setCanSign(canSignContract(contract, currentUser));
        dto.setCanFinish(canFinishContract(contract, currentUser));
        Object signerValue = redisService.hget("contract:signer", contract.getId());
        String signerId = signerValue == null ? null : String.valueOf(signerValue);
        dto.setSignerId(signerId);
        dto.setCanFinishBySigner(signerId != null && !signerId.equals(String.valueOf(currentUser.getId())));

        return dto;
    }

    /**
     * 甲方签订契约（冻结保证金并启动）
     */
    @Transactional
    public net.coding.template.entity.response.ContractSignResponse signContract(
            net.coding.template.entity.request.ContractSignRequest request, String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        Contract contract = contractMapper.selectContractDetail(request.getContractId());
        if (contract == null) {
            throw new BusinessException(404, "契约不存在");
        }

        if (!contract.getInitiatorId().equals(currentUser.getId())
                && (contract.getReceiverId() == null
                || !contract.getReceiverId().equals(currentUser.getId()))) {
            throw new BusinessException(403, "仅契约双方可签订");
        }

        if (!ContractStatus.PAID.getCode().equals(contract.getStatus())) {
            throw new BusinessException(400, "契约状态不允许签订");
        }

        if (contract.getDepositAmount() != null && contract.getDepositAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            net.coding.template.entity.request.FreezeRequest freezeRequest = new net.coding.template.entity.request.FreezeRequest();
            freezeRequest.setContractId(contract.getId());
            freezeRequest.setFreezeAmount(contract.getDepositAmount());
            paymentService.freezeDeposit(freezeRequest, token);
        }

        startContractAfterPayment(contract.getId());
        redisService.hset("contract:signer", contract.getId(), String.valueOf(currentUser.getId()));
        redisService.expire("contract:signer", 7, TimeUnit.DAYS);

        net.coding.template.entity.response.ContractSignResponse response = new net.coding.template.entity.response.ContractSignResponse();
        response.setContractId(contract.getId());
        response.setStatus(ContractStatus.ACTIVE.getCode());
        response.setMessage("签订成功");
        response.setSignTime(java.time.LocalDateTime.now());
        return response;
    }

    /**
     * 乙方完成契约（解冻并确认）
     */
    @Transactional
    public ContractConfirmResponse finishContract(
            net.coding.template.entity.request.ContractFinishRequest request,
            String token,
            HttpServletRequest httpRequest) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        Contract contract = contractMapper.selectContractDetail(request.getContractId());
        if (contract == null) {
            throw new BusinessException(404, "契约不存在");
        }

        if (contract.getReceiverId() == null || !contract.getReceiverId().equals(currentUser.getId())) {
            throw new BusinessException(403, "仅接收方可完成");
        }

        if (!ContractStatus.ACTIVE.getCode().equals(contract.getStatus())) {
            throw new BusinessException(400, "契约状态不允许完成");
        }

        if (contract.getDepositAmount() != null && contract.getDepositAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            net.coding.template.entity.request.UnfreezeRequest unfreezeRequest = new net.coding.template.entity.request.UnfreezeRequest();
            unfreezeRequest.setContractId(contract.getId());
            unfreezeRequest.setReason("契约完成解冻");
            paymentService.unfreezeDeposit(unfreezeRequest);
        }

        ContractConfirmRequest confirmRequest = new ContractConfirmRequest();
        confirmRequest.setContractId(contract.getId());
        confirmRequest.setEndTime(request.getEndTime());
        return confirmContract(confirmRequest, token, httpRequest);
    }

    /**
     * 确认完成契约
     */
    @Transactional
    public ContractConfirmResponse confirmContract(ContractConfirmRequest request, String token, HttpServletRequest httpRequest) {
        // 1. 获取当前用户
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 2. 查询契约
        Contract contract = contractMapper.selectContractDetail(request.getContractId());
        if (contract == null) {
            throw new BusinessException(404, "契约不存在");
        }

        // 3. 验证用户是否是参与方
        if (!contract.getInitiatorId().equals(currentUser.getId())
                && (contract.getReceiverId() == null
                || !contract.getReceiverId().equals(currentUser.getId()))) {
            throw new BusinessException(403, "无权操作此契约");
        }

        // 4. 验证契约状态
        if (!ContractStatus.ACTIVE.getCode().equals(contract.getStatus())) {
            throw new BusinessException(400, "契约状态不允许确认");
        }

        // 5. 查询确认记录
        ContractConfirm confirm = contractConfirmMapper.selectByContractAndUser(
                contract.getId(), currentUser.getId());
        if (confirm == null) {
            throw new BusinessException(400, "确认记录不存在");
        }

        // 6. 更新确认状态
        LocalDateTime confirmTime = LocalDateTime.now();
        confirm.setConfirmStatus(ConfirmStatus.CONFIRMED.getCode());
        confirm.setConfirmTime(confirmTime);
        confirm.setConfirmIp(getClientIp(httpRequest));
        contractConfirmMapper.updateById(confirm);

        // 7. 检查是否双方都已确认
        int confirmedCount = contractConfirmMapper.countConfirmed(contract.getId());

        if (confirmedCount == 2) {
            // 双方都已确认，完成契约
            completeContractInternal(contract, request.getEndTime());

            // 更新用户统计
            userMapper.incrementCompletedContracts(contract.getInitiatorId());
            userMapper.incrementCompletedContracts(contract.getReceiverId());

            // 增加信用分
            updateCreditScore(contract.getInitiatorId(), 10);
            updateCreditScore(contract.getReceiverId(), 10);

            // 解冻押金
            UnfreezeRequest unfreezeRequest = new UnfreezeRequest();
            unfreezeRequest.setContractId(contract.getId());
            paymentService.unfreezeDeposit(unfreezeRequest);

            log.info("契约完成: {}, 双方确认", contract.getContractNo());

            // 返回完成响应
            ContractConfirmResponse response = new ContractConfirmResponse();
            response.setContractId(contract.getId());
            response.setStatus("COMPLETED");
            response.setMessage("契约已完成，押金已解冻");
            response.setCompleteTime(confirmTime);
            return response;
        } else {
            // 只有一方确认，等待另一方
            log.info("契约部分确认: {}, 用户: {}", contract.getContractNo(), currentUser.getId());

            ContractConfirmResponse response = new ContractConfirmResponse();
            response.setContractId(contract.getId());
            response.setStatus("WAITING_OTHER");
            response.setMessage("已确认，等待对方确认");
            response.setConfirmTime(confirmTime);
            return response;
        }
    }

    /**
     * 开始契约（支付成功后调用）
     */
    @Transactional
    public void startContractAfterPayment(String contractId) {
        Contract contract = contractMapper.selectContractDetail(contractId);
        if (contract == null) {
            throw new BusinessException(404, "契约不存在");
        }

        // 更新状态为进行中
        contractMapper.updateContractStatus(contractId, ContractStatus.ACTIVE.getCode());
        contractMapper.updateContractPhase(contractId, ContractPhase.PREPARE.getCode());

        // 记录开始时间
        contractMapper.startContract(contractId, LocalDateTime.now());

        // 发送通知给双方
        sendContractStartNotification(contract);

        log.info("契约开始: {}", contract.getContractNo());
    }

    // ============ 私有方法 ============

    private String generateContractId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private BigDecimal calculateServiceFee(BigDecimal depositAmount) {
        // 根据押金金额计算服务费
        if (depositAmount.compareTo(new BigDecimal("50")) <= 0) {
            return new BigDecimal("1.00");
        } else if (depositAmount.compareTo(new BigDecimal("100")) <= 0) {
            return new BigDecimal("2.00");
        } else {
            return new BigDecimal("5.00");
        }
    }

    private void createConfirmRecords(Contract contract) {
        // 创建发起人确认记录
        ContractConfirm initiatorConfirm = new ContractConfirm();
        initiatorConfirm.setContractId(contract.getId());
        initiatorConfirm.setUserId(contract.getInitiatorId());
        initiatorConfirm.setUserRole("INITIATOR");
        initiatorConfirm.setConfirmStatus(ConfirmStatus.PENDING.getCode());
        contractConfirmMapper.insert(initiatorConfirm);

        // 创建接收人确认记录（只有接收人存在时才创建）
        if (contract.getReceiverId() != null) {
            ContractConfirm receiverConfirm = new ContractConfirm();
            receiverConfirm.setContractId(contract.getId());
            receiverConfirm.setUserId(contract.getReceiverId());
            receiverConfirm.setUserRole("RECEIVER");
            receiverConfirm.setConfirmStatus(ConfirmStatus.PENDING.getCode());
            contractConfirmMapper.insert(receiverConfirm);
        }
    }

    private ContractListResponse.ContractItem convertToContractItem(Contract contract) {
        ContractListResponse.ContractItem item = new ContractListResponse.ContractItem();
        item.setContractId(contract.getId());
        item.setContractNo(contract.getContractNo());
        item.setTitle(contract.getTitle());
        item.setGameType(contract.getGameType());
        item.setStatus(contract.getStatus());
        item.setDepositAmount(contract.getDepositAmount());
        item.setGuaranteeItem(contract.getGuaranteeItem());
        item.setMinCredit(contract.getMinCredit());
        item.setRequirements(contract.getRequirements());
        item.setDescription(contract.getDescription());
        item.setCoverUrl(contract.getCoverUrl());
        item.setSuccessCondition(contract.getSuccessCondition());
        item.setCreateTime(contract.getCreateTime());
        item.setCompleteTime(contract.getCompleteTime());

        User counterparty = null;
        if (contract.getReceiverId() != null) {
            counterparty = userMapper.selectById(contract.getReceiverId());
        }
        if (counterparty == null) {
            counterparty = userMapper.selectById(contract.getInitiatorId());
        }
        if (counterparty != null) {
            item.setCounterpartyNickname(counterparty.getNickname());
            item.setCounterpartyCreditScore(counterparty.getCreditScore());
            item.setOpponentCreditScore(counterparty.getCreditScore());
        }

        User initiator = userMapper.selectById(contract.getInitiatorId());
        if (initiator != null) {
            item.setInitiatorNickname(initiator.getNickname());
            item.setInitiatorCreditScore(initiator.getCreditScore());
        }

        return item;
    }

    private ContractDetailDTO.UserInfo buildUserInfo(User user) {
        if (user == null) return null;

        ContractDetailDTO.UserInfo info = new ContractDetailDTO.UserInfo();
        info.setUserId(user.getId());
        info.setNickname(user.getNickname());
        info.setAvatarUrl(user.getAvatarUrl());
        info.setCreditScore(user.getCreditScore());
        info.setIsVip(user.getIsVip());
        info.setCompletedContracts(user.getCompletedContracts());

        // 计算成功率
        if (user.getTotalContracts() > 0) {
            double successRate = (double) user.getCompletedContracts() / user.getTotalContracts() * 100;
            info.setSuccessRate(Math.round(successRate * 100.0) / 100.0);
        } else {
            info.setSuccessRate(100.0);
        }

        return info;
    }

    private ContractDetailDTO.ConfirmStatus buildConfirmStatus(List<ContractConfirm> confirms) {
        ContractDetailDTO.ConfirmStatus status = new ContractDetailDTO.ConfirmStatus();

        for (ContractConfirm confirm : confirms) {
            if ("INITIATOR".equals(confirm.getUserRole())) {
                status.setInitiatorConfirmed("CONFIRMED".equals(confirm.getConfirmStatus()));
                status.setInitiatorConfirmTime(confirm.getConfirmTime());
            } else if ("RECEIVER".equals(confirm.getUserRole())) {
                status.setReceiverConfirmed("CONFIRMED".equals(confirm.getConfirmStatus()));
                status.setReceiverConfirmTime(confirm.getConfirmTime());
            }
        }

        return status;
    }

    private boolean canConfirmContract(Contract contract, User currentUser, List<ContractConfirm> confirms) {
        if (!ContractStatus.ACTIVE.getCode().equals(contract.getStatus())) {
            return false;
        }

        // 检查是否已经确认过
        for (ContractConfirm confirm : confirms) {
            if (confirm.getUserId().equals(currentUser.getId())
                    && "CONFIRMED".equals(confirm.getConfirmStatus())) {
                return false;
            }
        }

        return true;
    }

    private boolean canCancelContract(Contract contract, User currentUser) {
        // 只有发起人且状态为PENDING或PAID时可以取消
        return contract.getInitiatorId().equals(currentUser.getId())
                && (ContractStatus.PENDING.getCode().equals(contract.getStatus())
                || ContractStatus.PAID.getCode().equals(contract.getStatus()));
    }

    private boolean canStartContract(Contract contract, User currentUser) {
        // 双方都可以在PAID状态时开始
        return (contract.getInitiatorId().equals(currentUser.getId())
                || (contract.getReceiverId() != null
                && contract.getReceiverId().equals(currentUser.getId())))
                && ContractStatus.PAID.getCode().equals(contract.getStatus());
    }

    private boolean canCompleteContract(Contract contract, User currentUser) {
        // 双方都可以在ACTIVE状态时完成
        return (contract.getInitiatorId().equals(currentUser.getId())
                || (contract.getReceiverId() != null
                && contract.getReceiverId().equals(currentUser.getId())))
                && ContractStatus.ACTIVE.getCode().equals(contract.getStatus());
    }

    private boolean canSignContract(Contract contract, User currentUser) {
        // 双方都可在PAID状态签订
        return (contract.getInitiatorId().equals(currentUser.getId())
                || (contract.getReceiverId() != null
                && contract.getReceiverId().equals(currentUser.getId())))
                && ContractStatus.PAID.getCode().equals(contract.getStatus());
    }

    private boolean canFinishContract(Contract contract, User currentUser) {
        // 仅接收方可在ACTIVE状态完成
        return contract.getReceiverId() != null
                && contract.getReceiverId().equals(currentUser.getId())
                && ContractStatus.ACTIVE.getCode().equals(contract.getStatus());
    }

    private boolean canDisputeContract(Contract contract, User currentUser) {
        // 双方都可以在ACTIVE状态时申请仲裁
        return (contract.getInitiatorId().equals(currentUser.getId())
                || (contract.getReceiverId() != null
                && contract.getReceiverId().equals(currentUser.getId())))
                && ContractStatus.ACTIVE.getCode().equals(contract.getStatus());
    }

    private void completeContractInternal(Contract contract, String endTimeStr) {
        LocalDateTime endTime = endTimeStr != null ?
                LocalDateTime.parse(endTimeStr) : LocalDateTime.now();

        contractMapper.completeContract(contract.getId(), endTime);
        contractMapper.updateContractStatus(contract.getId(), ContractStatus.COMPLETED.getCode());

        // 记录完成日志
        logContractComplete(contract);
    }

    private void updateCreditScore(Long userId, int addScore) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            int newScore = Math.min(100, user.getCreditScore() + addScore);
            userMapper.updateCreditScore(userId, newScore);

            // 记录信用分变动历史
            recordCreditHistory(userId, "CONTRACT_COMPLETE", addScore,
                    user.getCreditScore(), newScore, "完成契约");
        }
    }

    private void recordCreditHistory(Long userId, String changeType, int changeAmount,
                                     int beforeScore, int afterScore, String description) {
        // 实现信用分历史记录
        // 这里简化处理，实际应该调用CreditHistoryService
    }

    private void sendNotificationToReceiver(User receiver, Contract contract) {
        // 发送微信模板消息或站内信
        // 这里简化处理
        log.info("发送通知给接收方: {}, 契约: {}", receiver.getId(), contract.getContractNo());
    }

    private void sendContractStartNotification(Contract contract) {
        // 发送契约开始通知
        log.info("发送契约开始通知: {}", contract.getContractNo());
    }

    private void logContractComplete(Contract contract) {
        log.info("契约完成: {}, 完成时间: {}", contract.getContractNo(), LocalDateTime.now());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private Long getCurrentUserId() {
        // 获取当前登录用户ID，这里简化处理
        return 1L;
    }
}
