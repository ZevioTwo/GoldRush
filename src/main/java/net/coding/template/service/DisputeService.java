package net.coding.template.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.enums.OrderType;
import net.coding.template.entity.enums.PaymentStatus;
import net.coding.template.entity.po.Contract;
import net.coding.template.entity.po.Dispute;
import net.coding.template.entity.po.PaymentOrder;
import net.coding.template.entity.po.User;
import net.coding.template.entity.request.DeductRequest;
import net.coding.template.entity.request.DisputeApplyRequest;
import net.coding.template.entity.request.DisputeJudgeRequest;
import net.coding.template.entity.request.DisputeSubmitEvidenceRequest;
import net.coding.template.entity.response.DeductResponse;
import net.coding.template.entity.response.DisputeApplyResponse;
import net.coding.template.entity.response.DisputeJudgeResponse;
import net.coding.template.entity.response.DisputeSubmitResponse;
import net.coding.template.exception.BusinessException;
import net.coding.template.mapper.ContractMapper;
import net.coding.template.mapper.DisputeMapper;
import net.coding.template.mapper.PaymentOrderMapper;
import net.coding.template.mapper.UserMapper;
import net.coding.template.util.OrderNoGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class DisputeService {

    @Resource
    private DisputeMapper disputeMapper;

    @Resource
    private ContractMapper contractMapper;

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private PaymentService paymentService;

    @Resource
    private OrderNoGenerator orderNoGenerator;

    @Transactional
    public DisputeApplyResponse applyDispute(DisputeApplyRequest request, String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        Contract contract = contractMapper.selectContractDetail(request.getContractId());
        if (contract == null) {
            throw new BusinessException(404, "契约不存在");
        }

        boolean isParticipant = Objects.equals(contract.getInitiatorId(), currentUser.getId())
                || Objects.equals(contract.getReceiverId(), currentUser.getId());
        if (!isParticipant) {
            throw new BusinessException(403, "无权申请仲裁");
        }

        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new BusinessException(400, "仅进行中契约可申请仲裁");
        }

        if (Boolean.TRUE.equals(request.getIsUrgent())) {
            boolean urgentPaid = hasUrgentFeePaid(contract.getId(), currentUser.getId());
            if (!urgentPaid) {
                throw new BusinessException(400, "加急仲裁需先支付加急费");
            }
        }

        // 检查是否已存在未关闭争议
        List<Dispute> existing = disputeMapper.selectByContractId(contract.getId());
        if (!CollectionUtils.isEmpty(existing)) {
            for (Dispute d : existing) {
                if (!"RESOLVED".equals(d.getStatus()) && !"CLOSED".equals(d.getStatus())) {
                    throw new BusinessException(400, "该契约已存在进行中的仲裁");
                }
            }
        }

        Dispute dispute = new Dispute();
        dispute.setDisputeNo(orderNoGenerator.generate("ARB"));
        dispute.setContractId(contract.getId());
        dispute.setApplicantId(currentUser.getId());
        dispute.setRespondentId(Objects.equals(contract.getInitiatorId(), currentUser.getId())
                ? contract.getReceiverId() : contract.getInitiatorId());
        dispute.setApplicantRole(Objects.equals(contract.getInitiatorId(), currentUser.getId()) ? "INITIATOR" : "RECEIVER");
        dispute.setDisputeType(request.getDisputeType());
        dispute.setDescription(request.getDescription());
        dispute.setEvidenceUrls(toJson(request.getEvidenceUrls()));
        dispute.setGameScreenshotUrls(toJson(request.getGameScreenshotUrls()));
        dispute.setVideoLinks(toJson(request.getVideoLinks()));
        dispute.setStatus("PENDING");
        dispute.setIsUrgent(Boolean.TRUE.equals(request.getIsUrgent()));
        dispute.setUrgentFeePaid(Boolean.TRUE.equals(request.getIsUrgent()));

        disputeMapper.insert(dispute);

        // 契约进入争议状态
        contractMapper.enterDispute(contract.getId());

        // 记录争议次数
        userMapper.incrementDisputeCount(currentUser.getId());

        DisputeApplyResponse response = new DisputeApplyResponse();
        response.setDisputeNo(dispute.getDisputeNo());
        response.setContractId(contract.getId());
        response.setStatus(dispute.getStatus());
        response.setUrgent(dispute.getIsUrgent());
        response.setMessage("仲裁申请已提交");

        return response;
    }

    @Transactional
    public DisputeSubmitResponse submitEvidence(DisputeSubmitEvidenceRequest request, String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        Dispute dispute = disputeMapper.selectByDisputeNo(request.getDisputeNo());
        if (dispute == null) {
            throw new BusinessException(404, "仲裁记录不存在");
        }

        boolean isParticipant = Objects.equals(dispute.getApplicantId(), currentUser.getId())
                || Objects.equals(dispute.getRespondentId(), currentUser.getId());
        if (!isParticipant) {
            throw new BusinessException(403, "无权提交证据");
        }

        if (!"PENDING".equals(dispute.getStatus()) && !"PROCESSING".equals(dispute.getStatus())) {
            throw new BusinessException(400, "当前状态无法提交证据");
        }

        // 简化处理：追加/覆盖证据与描述
        if (request.getDescription() != null) {
            dispute.setDescription(request.getDescription());
        }
        if (request.getEvidenceUrls() != null) {
            dispute.setEvidenceUrls(toJson(request.getEvidenceUrls()));
        }
        if (request.getGameScreenshotUrls() != null) {
            dispute.setGameScreenshotUrls(toJson(request.getGameScreenshotUrls()));
        }
        if (request.getVideoLinks() != null) {
            dispute.setVideoLinks(toJson(request.getVideoLinks()));
        }
        dispute.setStatus("PROCESSING");
        dispute.setUpdateTime(LocalDateTime.now());
        disputeMapper.updateById(dispute);

        DisputeSubmitResponse response = new DisputeSubmitResponse();
        response.setDisputeNo(dispute.getDisputeNo());
        response.setStatus(dispute.getStatus());
        response.setMessage("证据已提交");
        return response;
    }

    @Transactional
    public DisputeJudgeResponse judgeDispute(DisputeJudgeRequest request, Long handlerId) {
        Dispute dispute = disputeMapper.selectByDisputeNo(request.getDisputeNo());
        if (dispute == null) {
            throw new BusinessException(404, "仲裁记录不存在");
        }

        if ("RESOLVED".equals(dispute.getStatus()) || "CLOSED".equals(dispute.getStatus())) {
            throw new BusinessException(400, "仲裁已结案");
        }

        if (!"PENDING".equals(dispute.getStatus()) && !"PROCESSING".equals(dispute.getStatus())) {
            throw new BusinessException(400, "当前状态不允许判责");
        }

        Contract contract = contractMapper.selectContractDetail(dispute.getContractId());
        if (contract == null) {
            throw new BusinessException(404, "契约不存在");
        }

        if (!"DISPUTE".equals(contract.getStatus())) {
            throw new BusinessException(400, "契约非争议状态，无法判责");
        }

        if (Boolean.TRUE.equals(request.getViolation())) {
            if (!"APPLICANT_WIN".equals(request.getResult()) && !"RESPONDENT_WIN".equals(request.getResult())) {
                throw new BusinessException(400, "违约判定结果不合法");
            }
        } else {
            if (!"DISMISSED".equals(request.getResult())) {
                throw new BusinessException(400, "无违约时仅支持驳回仲裁");
            }
        }

        // 更新仲裁结果
        disputeMapper.updateJudgeResult(
                dispute.getId(),
                "RESOLVED",
                request.getResult(),
                request.getResultReason(),
                handlerId
        );

        // 若判定违约则扣罚并更新契约状态
        if (Boolean.TRUE.equals(request.getViolation())) {
            Long violatorUserId = resolveViolatorUserId(dispute, request.getResult());
            Long victimUserId = resolveVictimUserId(dispute, request.getResult());

            if (violatorUserId == null || victimUserId == null) {
                throw new BusinessException(400, "无法识别违约方或受害方");
            }

            BigDecimal deductAmount = request.getDeductAmount();
            if (deductAmount == null) {
                // 默认按押金全额扣除
                deductAmount = contract.getDepositAmount();
            }

            if (deductAmount == null || deductAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "扣除金额不合法");
            }

            if (contract.getDepositAmount() != null && deductAmount.compareTo(contract.getDepositAmount()) > 0) {
                throw new BusinessException(400, "扣除金额超过押金上限");
            }

            DeductRequest deductRequest = new DeductRequest();
            deductRequest.setContractId(contract.getId());
            deductRequest.setViolatorUserId(violatorUserId);
            deductRequest.setVictimUserId(victimUserId);
            deductRequest.setDeductAmount(deductAmount);
            deductRequest.setReason(request.getResultReason());

            DeductResponse deductResponse = paymentService.deductPenalty(deductRequest);
            if (deductResponse == null || !Boolean.TRUE.equals(deductResponse.getSuccess())) {
                throw new BusinessException(500, "违约金扣除失败");
            }
        } else {
            // 驳回仲裁：恢复为进行中
            contractMapper.updateContractStatus(contract.getId(), "ACTIVE");
        }

        DisputeJudgeResponse response = new DisputeJudgeResponse();
        response.setDisputeNo(dispute.getDisputeNo());
        response.setContractId(contract.getId());
        response.setStatus("RESOLVED");
        response.setResult(request.getResult());
        response.setMessage("仲裁已判责");
        return response;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        return JSON.toJSONString(obj);
    }

    private boolean hasUrgentFeePaid(String contractId, Long userId) {
        List<PaymentOrder> orders = paymentOrderMapper.selectByContractAndType(
                contractId, OrderType.ARBITRATION_FEE.getCode());
        if (CollectionUtils.isEmpty(orders)) {
            return false;
        }
        return orders.stream()
                .anyMatch(o -> Objects.equals(o.getUserId(), userId)
                        && PaymentStatus.SUCCESS.getCode().equals(o.getPaymentStatus()));
    }

    private Long resolveViolatorUserId(Dispute dispute, String result) {
        if ("APPLICANT_WIN".equals(result)) {
            return dispute.getRespondentId();
        }
        if ("RESPONDENT_WIN".equals(result)) {
            return dispute.getApplicantId();
        }
        return null;
    }

    private Long resolveVictimUserId(Dispute dispute, String result) {
        if ("APPLICANT_WIN".equals(result)) {
            return dispute.getApplicantId();
        }
        if ("RESPONDENT_WIN".equals(result)) {
            return dispute.getRespondentId();
        }
        return null;
    }
}
