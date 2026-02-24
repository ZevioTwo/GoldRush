package net.coding.template.controller;

import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.request.ContractConfirmRequest;
import net.coding.template.entity.request.ContractCreateRequest;
import net.coding.template.entity.dto.ContractDetailDTO;
import net.coding.template.entity.request.ContractListRequest;
import net.coding.template.entity.request.ContractAcceptRequest;
import net.coding.template.entity.response.CommonResponse;
import net.coding.template.entity.response.ContractConfirmResponse;
import net.coding.template.entity.response.ContractCreateResponse;
import net.coding.template.entity.response.ContractListResponse;
import net.coding.template.exception.BusinessException;
import net.coding.template.service.ContractService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/contract")
public class ContractController {

    @Resource
    private ContractService contractService;

    /**
     * 创建契约
     * POST /api/contract/create
     */
    @PostMapping("/create")
    public CommonResponse<ContractCreateResponse> createContract(
            @Valid @RequestBody ContractCreateRequest request,
            @RequestHeader("Authorization") String token,
            HttpServletRequest httpRequest) {

        try {
            // 验证token格式
            if (token == null || !token.startsWith("Bearer ")) {
                throw new BusinessException(401, "token格式错误");
            }

            String accessToken = token.substring(7);

            log.info("创建契约请求: {}, 用户Token: {}",
                    request.getReceiverGameId(), accessToken.substring(0, 10) + "...");

            ContractCreateResponse response = contractService.createContract(request, accessToken);

            log.info("契约创建成功: {}", response.getContractNo());

            return CommonResponse.success("契约创建成功", response);

        } catch (BusinessException e) {
            log.warn("创建契约失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("创建契约异常", e);
            throw new BusinessException(500, "创建契约失败: " + e.getMessage());
        }
    }

    /**
     * 获取契约列表
     * GET /api/contract/list
     */
    @GetMapping("/list")
    public CommonResponse<ContractListResponse> getContractList(
            @Valid ContractListRequest request,
            @RequestHeader("Authorization") String token) {

        try {
            if (token == null || !token.startsWith("Bearer ")) {
                throw new BusinessException(401, "token格式错误");
            }

            String accessToken = token.substring(7);

            // 参数验证
            if (request.getPage() == null || request.getPage() < 1) {
                request.setPage(1);
            }
            if (request.getSize() == null || request.getSize() < 1 || request.getSize() > 100) {
                request.setSize(20);
            }

            log.info("查询契约列表: page={}, size={}, status={}",
                    request.getPage(), request.getSize(), request.getStatus());

            ContractListResponse response = contractService.getContractList(request, accessToken);

            log.info("契约列表查询成功: 共{}条", response.getTotal());

            return CommonResponse.success(response);

        } catch (BusinessException e) {
            log.warn("查询契约列表失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("查询契约列表异常", e);
            throw new BusinessException(500, "查询契约列表失败");
        }
    }

    /**
     * 获取契约详情
     * GET /api/contract/{id}
     */
    @GetMapping("/{id}")
    public CommonResponse<ContractDetailDTO> getContractDetail(
            @PathVariable("id") String contractId,
            @RequestHeader("Authorization") String token) {

        try {
            if (token == null || !token.startsWith("Bearer ")) {
                throw new BusinessException(401, "token格式错误");
            }

            String accessToken = token.substring(7);

            log.info("查询契约详情: {}", contractId);

            ContractDetailDTO detail = contractService.getContractDetail(contractId, accessToken);

            log.info("契约详情查询成功: {}", contractId);

            return CommonResponse.success(detail);

        } catch (BusinessException e) {
            log.warn("查询契约详情失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("查询契约详情异常", e);
            throw new BusinessException(500, "查询契约详情失败");
        }
    }

    /**
     * 确认完成契约
     * POST /api/contract/confirm
     */
    @PostMapping("/confirm")
    public CommonResponse<ContractConfirmResponse> confirmContract(
            @Valid @RequestBody ContractConfirmRequest request,
            @RequestHeader("Authorization") String token,
            HttpServletRequest httpRequest) {

        try {
            if (token == null || !token.startsWith("Bearer ")) {
                throw new BusinessException(401, "token格式错误");
            }

            String accessToken = token.substring(7);

            log.info("确认契约完成: {}", request.getContractId());

            ContractConfirmResponse response = contractService.confirmContract(
                    request, accessToken, httpRequest);

            log.info("契约确认结果: {} - {}", request.getContractId(), response.getStatus());

            return CommonResponse.success("确认成功", response);

        } catch (BusinessException e) {
            log.warn("确认契约失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("确认契约异常", e);
            throw new BusinessException(500, "确认契约失败");
        }
    }

    /**
     * 开始契约（内部调用）
     * POST /api/contract/start
     */
    @PostMapping("/start/{contractId}")
    public CommonResponse<Void> startContract(
            @PathVariable("id") String contractId,
            @RequestHeader("X-Internal-Key") String internalKey) {

        try {
            // 验证内部调用权限
            if (!"INTERNAL_SECRET_KEY".equals(internalKey)) {
                throw new BusinessException(403, "无权访问");
            }

            log.info("内部调用开始契约: {}", contractId);

            contractService.startContractAfterPayment(contractId);

            log.info("契约开始成功: {}", contractId);

            return CommonResponse.success("契约开始成功", null);

        } catch (Exception e) {
            log.error("开始契约异常", e);
            throw new BusinessException(500, "开始契约失败");
        }
    }

    /**
     * 取消契约
     * POST /api/contract/cancel
     */
    @PostMapping("/cancel")
    public CommonResponse<Void> cancelContract(
            @RequestParam("contractId") String contractId,
            @RequestHeader("Authorization") String token) {

        try {
            // TODO: 实现取消逻辑
            return CommonResponse.success("取消成功", null);
        } catch (Exception e) {
            log.error("取消契约异常", e);
            throw new BusinessException(500, "取消契约失败");
        }
    }

    /**
     * 契约大厅：待接单列表
     * GET /api/contract/hall
     */
    @GetMapping("/hall")
    public CommonResponse<ContractListResponse> getHallList(
            @Valid ContractListRequest request) {
        try {
            ContractListResponse response = contractService.getHallList(request);
            return CommonResponse.success(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询契约大厅异常", e);
            throw new BusinessException(500, "查询契约大厅失败");
        }
    }

    /**
     * 接单
     * POST /api/contract/accept
     */
    @PostMapping("/accept")
    public CommonResponse<Void> acceptContract(
            @Valid @RequestBody ContractAcceptRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                throw new BusinessException(401, "token格式错误");
            }
            String accessToken = token.substring(7);

            contractService.acceptContract(request, accessToken);
            return CommonResponse.success("接单成功", null);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("接单异常", e);
            throw new BusinessException(500, "接单失败");
        }
    }
}
