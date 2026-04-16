package net.coding.template.controller;

import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.request.MessageSendRequest;
import net.coding.template.entity.response.CommonResponse;
import net.coding.template.entity.response.MessageOpenSessionResponse;
import net.coding.template.entity.response.MessageSessionDetailResponse;
import net.coding.template.entity.response.MessageSessionListResponse;
import net.coding.template.exception.BusinessException;
import net.coding.template.service.MessageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Resource
    private MessageService messageService;

    @GetMapping("/sessions")
    public CommonResponse<MessageSessionListResponse> getSessions(@RequestHeader("Authorization") String token) {
        try {
            return CommonResponse.success(messageService.getSessions(extractToken(token)));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询消息会话失败", e);
            throw new BusinessException(500, "查询消息会话失败");
        }
    }

    @GetMapping("/sessions/{id}")
    public CommonResponse<MessageSessionDetailResponse> getSessionDetail(@PathVariable("id") Long sessionId,
                                                                        @RequestHeader("Authorization") String token) {
        try {
            return CommonResponse.success(messageService.getSessionDetail(sessionId, extractToken(token)));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询消息详情失败", e);
            throw new BusinessException(500, "查询消息详情失败");
        }
    }

    @PostMapping("/sessions/{id}/read")
    public CommonResponse<Void> markSessionRead(@PathVariable("id") Long sessionId,
                                                @RequestHeader("Authorization") String token) {
        try {
            messageService.markSessionRead(sessionId, extractToken(token));
            return CommonResponse.success("已标记为已读", null);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("标记消息已读失败", e);
            throw new BusinessException(500, "标记消息已读失败");
        }
    }

    @PostMapping("/sessions/{id}/send")
    public CommonResponse<Void> sendMessage(@PathVariable("id") Long sessionId,
                                            @Valid @RequestBody MessageSendRequest request,
                                            @RequestHeader("Authorization") String token) {
        try {
            messageService.sendMessage(sessionId, request, extractToken(token));
            return CommonResponse.success("发送成功", null);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("发送消息失败", e);
            throw new BusinessException(500, "发送消息失败");
        }
    }

    @PostMapping("/contract/{contractId}/session")
    public CommonResponse<MessageOpenSessionResponse> openContractSession(@PathVariable("contractId") String contractId,
                                                                         @RequestHeader("Authorization") String token) {
        try {
            return CommonResponse.success(messageService.openContractSession(contractId, extractToken(token)));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("打开契约私聊失败", e);
            throw new BusinessException(500, "打开契约私聊失败");
        }
    }

    private String extractToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(401, "token格式错误");
        }
        return token.substring(7);
    }
}
