package net.coding.template.controller;

import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.request.BountyCreateRequest;
import net.coding.template.entity.request.BountyListRequest;
import net.coding.template.entity.response.BountyClaimResponse;
import net.coding.template.entity.response.BountyCreateResponse;
import net.coding.template.entity.response.BountyDetailResponse;
import net.coding.template.entity.response.BountyListResponse;
import net.coding.template.entity.response.CommonResponse;
import net.coding.template.exception.BusinessException;
import net.coding.template.service.BountyService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/bounty")
public class BountyController {

    @Resource
    private BountyService bountyService;

    @PostMapping("/create")
    public CommonResponse<BountyCreateResponse> createBounty(@Valid @RequestBody BountyCreateRequest request,
                                                             @RequestHeader("Authorization") String token) {
        try {
            String accessToken = extractToken(token);
            BountyCreateResponse response = bountyService.createBounty(request, accessToken);
            return CommonResponse.success("悬赏发布成功", response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("发起悬赏失败", e);
            throw new BusinessException(500, "发起悬赏失败");
        }
    }

    @GetMapping("/list")
    public CommonResponse<BountyListResponse> getBountyList(@Valid BountyListRequest request,
                                                            @RequestHeader("Authorization") String token) {
        try {
            String accessToken = extractToken(token);
            return CommonResponse.success(bountyService.getBountyList(request, accessToken));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询悬赏榜单失败", e);
            throw new BusinessException(500, "查询悬赏榜单失败");
        }
    }

    @GetMapping("/{id}")
    public CommonResponse<BountyDetailResponse> getBountyDetail(@PathVariable("id") Long bountyId,
                                                                @RequestHeader("Authorization") String token) {
        try {
            String accessToken = extractToken(token);
            return CommonResponse.success(bountyService.getBountyDetail(bountyId, accessToken));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询悬赏详情失败", e);
            throw new BusinessException(500, "查询悬赏详情失败");
        }
    }

    @PostMapping("/{id}/claim")
    public CommonResponse<BountyClaimResponse> claimBounty(@PathVariable("id") Long bountyId,
                                                           @RequestHeader("Authorization") String token) {
        try {
            String accessToken = extractToken(token);
            BountyClaimResponse response = bountyService.claimBounty(bountyId, accessToken);
            return CommonResponse.success("接取悬赏成功", response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("接取悬赏失败", e);
            throw new BusinessException(500, "接取悬赏失败");
        }
    }

    private String extractToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(401, "token格式错误");
        }
        return token.substring(7);
    }
}
