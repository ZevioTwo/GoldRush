package net.coding.template.controller;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.request.*;
import net.coding.template.entity.response.*;
import net.coding.template.exception.BusinessException;
import net.coding.template.service.PaymentService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    /**
     * 1. 生成支付预订单
     * POST /api/payment/prepay
     */
    @PostMapping("/prepay")
    public CommonResponse<PaymentResponse> prepay(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("Authorization") String token) {

        try {
            log.info("生成支付预订单请求: contractId={}, orderType={}, amount={}",
                    request.getContractId(), request.getOrderType(), request.getAmount());

            PaymentResponse response = paymentService.prepay(request, token);

            if (response.getSuccess()) {
                log.info("预订单生成成功: orderNo={}", response.getOrderNo());
                return CommonResponse.success("预订单生成成功", response);
            } else {
                log.warn("预订单生成失败: {}", response.getMessage());
                return CommonResponse.error(400, response.getMessage());
            }

        } catch (BusinessException e) {
            log.warn("生成支付预订单失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("生成支付预订单异常", e);
            throw new BusinessException(500, "生成支付预订单失败");
        }
    }

    /**
     * 2. 微信支付回调
     * POST /api/payment/notify
     */
    @PostMapping("/notify")
    public Map<String, String> handlePaymentNotify(
            @RequestBody String requestBody,
            HttpServletRequest httpRequest) {

        Map<String, String> result = new HashMap<>();
        try {
            PaymentNotifyRequest request = JSON.parseObject(requestBody, PaymentNotifyRequest.class);

            // 从请求头获取签名信息
            String timestamp = httpRequest.getHeader("Wechatpay-Timestamp");
            String nonce = httpRequest.getHeader("Wechatpay-Nonce");
            String signature = httpRequest.getHeader("Wechatpay-Signature");
            String serial = httpRequest.getHeader("Wechatpay-Serial");

            log.info("收到支付回调: timestamp={}, nonce={}, serial={}", timestamp, nonce, serial);

            // 设置签名信息
            request.setTimestamp(timestamp);
            request.setNonce(nonce);
            request.setSignature(signature);
            request.setBody(requestBody);

            // 处理回调
            paymentService.handlePaymentNotify(request);

            // 返回成功响应给微信
            result.put("code", "SUCCESS");
            result.put("message", "处理成功");
            return result;

        } catch (BusinessException e) {
            log.error("支付回调处理失败: {}", e.getMessage());

            result.put("code", "FAIL");
            result.put("message", e.getMessage());
            return result;
        } catch (Exception e) {
            log.error("支付回调处理异常", e);
            result.put("code", "FAIL");
            result.put("message", "系统异常");
            return result;
        }
    }

    /**
     * 3. 资金冻结
     * POST /api/payment/freeze
     */
    @PostMapping("/freeze")
    public CommonResponse<FreezeResponse> freezeDeposit(
            @Valid @RequestBody FreezeRequest request,
            @RequestHeader("Authorization") String token) {

        try {
            log.info("资金冻结请求: contractId={}, amount={}",
                    request.getContractId(), request.getFreezeAmount());

            FreezeResponse response = paymentService.freezeDeposit(request, token);

            if (response.getSuccess()) {
                log.info("资金冻结成功: orderNo={}, authCode={}",
                        response.getOrderNo(), response.getAuthorizationCode());
                return CommonResponse.success("资金冻结成功", response);
            } else {
                log.warn("资金冻结失败: {}", response.getMessage());
                return CommonResponse.error(400, response.getMessage());
            }

        } catch (BusinessException e) {
            log.warn("资金冻结失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("资金冻结异常", e);
            throw new BusinessException(500, "资金冻结失败");
        }
    }

    /**
     * 4. 资金解冻
     * POST /api/payment/unfreeze
     */
    @PostMapping("/unfreeze")
    public CommonResponse<UnfreezeResponse> unfreezeDeposit(
            @Valid @RequestBody UnfreezeRequest request) {

        try {
            log.info("资金解冻请求: contractId={}", request.getContractId());

            UnfreezeResponse response = paymentService.unfreezeDeposit(request);

            if (response.getSuccess()) {
                // 统计成功解冻的数量
                long successCount = response.getResults().stream()
                        .filter(UnfreezeResponse.UnfreezeResult::getSuccess)
                        .count();

                log.info("资金解冻完成: contractId={}, 成功解冻{}笔",
                        request.getContractId(), successCount);

                return CommonResponse.success("资金解冻处理完成", response);
            } else {
                log.warn("资金解冻失败: {}", response.getMessage());
                return CommonResponse.error(400, response.getMessage());
            }

        } catch (BusinessException e) {
            log.warn("资金解冻失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("资金解冻异常", e);
            throw new BusinessException(500, "资金解冻失败");
        }
    }

    /**
     * 5. 扣除违约金
     * POST /api/payment/deduct
     */
    @PostMapping("/deduct")
    public CommonResponse<DeductResponse> deductPenalty(
            @Valid @RequestBody DeductRequest request,
            @RequestHeader("Authorization") String token) {

        try {
            log.info("扣除违约金请求: contractId={}, violator={}, victim={}, amount={}",
                    request.getContractId(), request.getViolatorUserId(),
                    request.getVictimUserId(), request.getDeductAmount());

            // 验证操作权限（通常需要管理员或仲裁员权限）
            validateDeductPermission(token);

            DeductResponse response = paymentService.deductPenalty(request);

            if (response.getSuccess()) {
                log.info("违约金扣除成功: deductOrder={}, compOrder={}",
                        response.getDeductOrderId(), response.getCompensationOrderId());
                return CommonResponse.success("违约金扣除成功", response);
            } else {
                log.warn("违约金扣除失败: {}", response.getMessage());
                return CommonResponse.error(400, response.getMessage());
            }

        } catch (BusinessException e) {
            log.warn("扣除违约金失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("扣除违约金异常", e);
            throw new BusinessException(500, "扣除违约金失败");
        }
    }

    /**
     * 查询支付状态
     * GET /api/payment/status/{orderNo}
     */
    @GetMapping("/status/{orderNo}")
    public CommonResponse<Map<String, Object>> getPaymentStatus(
            @PathVariable String orderNo,
            @RequestHeader("Authorization") String token) {
        try {
            return CommonResponse.success("查询成功", paymentService.getPaymentStatus(orderNo, token));
        } catch (BusinessException e) {
            log.warn("查询支付状态失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("查询支付状态异常", e);
            throw new BusinessException(500, "查询支付状态失败");
        }
    }

    /**
     * 验证扣除权限
     */
    private void validateDeductPermission(String token) {
        // 实现权限验证逻辑
        // 这里简化处理，实际需要根据token验证用户角色
        // 只有管理员或仲裁员可以扣除违约金
        throw new BusinessException(403, "无权进行扣款操作");
    }
}
