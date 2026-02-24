package net.coding.template.controller;

import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.request.DisputeApplyRequest;
import net.coding.template.entity.request.DisputeJudgeRequest;
import net.coding.template.entity.request.DisputeSubmitEvidenceRequest;
import net.coding.template.entity.response.CommonResponse;
import net.coding.template.entity.response.DisputeApplyResponse;
import net.coding.template.entity.response.DisputeJudgeResponse;
import net.coding.template.entity.response.DisputeSubmitResponse;
import net.coding.template.exception.BusinessException;
import net.coding.template.service.DisputeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/dispute")
public class DisputeController {

    @Resource
    private DisputeService disputeService;

    /**
     * 申请仲裁
     * POST /api/dispute/apply
     */
    @PostMapping("/apply")
    public CommonResponse<DisputeApplyResponse> apply(
            @Valid @RequestBody DisputeApplyRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                throw new BusinessException(401, "token格式错误");
            }
            String accessToken = token.substring(7);
            DisputeApplyResponse response = disputeService.applyDispute(request, accessToken);
            return CommonResponse.success("仲裁申请成功", response);
        } catch (BusinessException e) {
            log.warn("仲裁申请失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("仲裁申请异常", e);
            throw new BusinessException(500, "仲裁申请失败");
        }
    }

    /**
     * 提交证据
     * POST /api/dispute/submit
     */
    @PostMapping("/submit")
    public CommonResponse<DisputeSubmitResponse> submit(
            @Valid @RequestBody DisputeSubmitEvidenceRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                throw new BusinessException(401, "token格式错误");
            }
            String accessToken = token.substring(7);
            DisputeSubmitResponse response = disputeService.submitEvidence(request, accessToken);
            return CommonResponse.success("证据提交成功", response);
        } catch (BusinessException e) {
            log.warn("证据提交失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("证据提交异常", e);
            throw new BusinessException(500, "证据提交失败");
        }
    }

    /**
     * 人工判责（后台）
     * POST /api/dispute/judge
     */
    @PostMapping("/judge")
    public CommonResponse<DisputeJudgeResponse> judge(
            @Valid @RequestBody DisputeJudgeRequest request,
            @RequestHeader("X-Admin-Id") Long handlerId) {
        try {
            if (handlerId == null) {
                throw new BusinessException(401, "缺少处理人ID");
            }
            DisputeJudgeResponse response = disputeService.judgeDispute(request, handlerId);
            return CommonResponse.success("仲裁判责完成", response);
        } catch (BusinessException e) {
            log.warn("仲裁判责失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("仲裁判责异常", e);
            throw new BusinessException(500, "仲裁判责失败");
        }
    }
}
