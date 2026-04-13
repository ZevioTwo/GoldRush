package net.coding.template.controller;

import net.coding.template.entity.dto.CreditRankingDTO;
import net.coding.template.entity.dto.CreditScoreDTO;
import net.coding.template.entity.request.LoginRequest;
import net.coding.template.entity.dto.UserProfileDTO;
import net.coding.template.entity.dto.UserStatsDTO;
import net.coding.template.entity.response.CommonResponse;
import net.coding.template.exception.BusinessException;
import net.coding.template.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private net.coding.template.service.UserGameAccountService userGameAccountService;

    /**
     * 微信登录接口
     * POST /api/user/login
     */
    @PostMapping("/login")
    public CommonResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("用户登录请求: {}", request.getCode() != null ? "code已提供" : "code为空");

            Map<String, Object> result = userService.login(request);

            // 记录登录日志
            log.info("用户登录成功: {}", result.get("userId"));

            return CommonResponse.success("登录成功", result);
        } catch (Exception e) {
            log.error("登录失败", e);
            throw new BusinessException(401, "登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户信息
     * GET /api/user/profile
     * 需要token验证
     */
    @GetMapping("/profile")
    public CommonResponse<UserProfileDTO> getProfile(HttpServletRequest request) {
        try {
            // 从header获取token
            String token = extractToken(request);
            if (token == null) {
                throw new BusinessException(401, "未提供认证token");
            }

            // 获取当前用户
            Long userId = userService.getUserIdByToken(token);
            if (userId == null) {
                throw new BusinessException(401, "token无效或已过期");
            }

            UserProfileDTO profile = userService.getUserProfile(userId);

            log.info("获取用户信息: {}", userId);

            return CommonResponse.success(profile);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            throw new BusinessException(500, "获取用户信息失败");
        }
    }

    /**
     * 查询信用分
     * GET /api/user/credit
     * 需要token验证
     */
    @GetMapping("/credit")
    public CommonResponse<CreditScoreDTO> getCreditScore(HttpServletRequest request) {
        try {
            // 从header获取token
            String token = extractToken(request);
            if (token == null) {
                throw new BusinessException(401, "未提供认证token");
            }

            // 获取当前用户
            Long userId = userService.getUserIdByToken(token);
            if (userId == null) {
                throw new BusinessException(401, "token无效或已过期");
            }

            CreditScoreDTO creditScore = userService.getCreditScore(userId);

            log.info("查询用户信用分: {} - {}", userId, creditScore.getCurrentScore());

            return CommonResponse.success(creditScore);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询信用分失败", e);
            throw new BusinessException(500, "查询信用分失败");
        }
    }

    /**
     * 获取信誉排行榜
     * GET /api/user/ranking
     */
    @GetMapping("/ranking")
    public CommonResponse<CreditRankingDTO> getCreditRanking(
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        try {
            CreditRankingDTO ranking = userService.getCreditRanking(limit);
            log.info("获取信誉排行榜成功: limit={}, redCount={}, blackCount={}",
                    limit,
                    ranking.getRedList().size(),
                    ranking.getBlackList().size());
            return CommonResponse.success(ranking);
        } catch (Exception e) {
            log.error("获取信誉排行榜失败", e);
            throw new BusinessException(500, "获取信誉排行榜失败");
        }
    }

    /**
     * 获取用户中心统计
     * GET /api/user/stats
     */
    @GetMapping("/stats")
    public CommonResponse<UserStatsDTO> getUserStats(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            if (token == null) {
                throw new BusinessException(401, "未提供认证token");
            }

            Long userId = userService.getUserIdByToken(token);
            if (userId == null) {
                throw new BusinessException(401, "token无效或已过期");
            }

            UserStatsDTO stats = userService.getUserStats(userId);
            return CommonResponse.success(stats);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户统计失败", e);
            throw new BusinessException(500, "获取用户统计失败");
        }
    }

    /**
     * 获取签到状态
     * GET /api/user/checkin/status
     */
    @GetMapping("/checkin/status")
    public CommonResponse<Map<String, Object>> getCheckinStatus(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            if (token == null) {
                throw new BusinessException(401, "未提供认证token");
            }
            Long userId = userService.getUserIdByToken(token);
            if (userId == null) {
                throw new BusinessException(401, "token无效或已过期");
            }

            return CommonResponse.success(userService.getCheckinStatus(userId));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取签到状态失败", e);
            throw new BusinessException(500, "获取签到状态失败");
        }
    }

    /**
     * 签到领取
     * POST /api/user/checkin/claim
     */
    @PostMapping("/checkin/claim")
    public CommonResponse<Map<String, Object>> claimCheckin(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            if (token == null) {
                throw new BusinessException(401, "未提供认证token");
            }
            Long userId = userService.getUserIdByToken(token);
            if (userId == null) {
                throw new BusinessException(401, "token无效或已过期");
            }

            return CommonResponse.success(userService.claimCheckin(userId));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("签到失败", e);
            throw new BusinessException(500, "签到失败");
        }
    }

    /**
     * 完善资料（微信号/手机号）
     * POST /api/user/profile/update
     */
    @PostMapping("/profile/update")
    public CommonResponse<Void> updateProfile(@Valid @RequestBody net.coding.template.entity.request.UserProfileUpdateRequest request,
                                              HttpServletRequest httpRequest) {
        try {
            String token = extractToken(httpRequest);
            if (token == null) {
                throw new BusinessException(401, "未提供认证token");
            }

            Long userId = userService.getUserIdByToken(token);
            if (userId == null) {
                throw new BusinessException(401, "token无效或已过期");
            }

            userService.updateProfile(userId, request.getWechatId(), request.getPhone());

            return CommonResponse.success("更新成功", null);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            throw new BusinessException(500, "更新用户信息失败");
        }
    }

    /**
     * 获取用户游戏账号列表
     * GET /api/user/game-accounts
     */
    @GetMapping("/game-accounts")
    public CommonResponse<java.util.List<net.coding.template.entity.response.UserGameAccountResponse>> listGameAccounts(HttpServletRequest httpRequest) {
        try {
            String token = extractToken(httpRequest);
            if (token == null) {
                throw new BusinessException(401, "未提供认证token");
            }
            Long userId = userService.getUserIdByToken(token);
            if (userId == null) {
                throw new BusinessException(401, "token无效或已过期");
            }

            return CommonResponse.success(userGameAccountService.listByUser(userId));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取账号列表失败", e);
            throw new BusinessException(500, "获取账号列表失败");
        }
    }

    /**
     * 新增用户游戏账号
     * POST /api/user/game-accounts
     */
    @PostMapping("/game-accounts")
    public CommonResponse<net.coding.template.entity.response.UserGameAccountResponse> createGameAccount(
            @Valid @RequestBody net.coding.template.entity.request.UserGameAccountCreateRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = extractToken(httpRequest);
            if (token == null) {
                throw new BusinessException(401, "未提供认证token");
            }
            Long userId = userService.getUserIdByToken(token);
            if (userId == null) {
                throw new BusinessException(401, "token无效或已过期");
            }

            return CommonResponse.success("新增成功", userGameAccountService.create(userId, request));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("新增账号失败", e);
            throw new BusinessException(500, "新增账号失败");
        }
    }

    /**
     * 更新用户游戏账号
     * PUT /api/user/game-accounts/{id}
     */
    @PutMapping("/game-accounts/{id}")
    public CommonResponse<net.coding.template.entity.response.UserGameAccountResponse> updateGameAccount(
            @PathVariable("id") Long id,
            @Valid @RequestBody net.coding.template.entity.request.UserGameAccountUpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = extractToken(httpRequest);
            if (token == null) {
                throw new BusinessException(401, "未提供认证token");
            }
            Long userId = userService.getUserIdByToken(token);
            if (userId == null) {
                throw new BusinessException(401, "token无效或已过期");
            }

            return CommonResponse.success("更新成功", userGameAccountService.update(userId, id, request));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新账号失败", e);
            throw new BusinessException(500, "更新账号失败");
        }
    }

    /**
     * 删除用户游戏账号
     * DELETE /api/user/game-accounts/{id}
     */
    @DeleteMapping("/game-accounts/{id}")
    public CommonResponse<Void> deleteGameAccount(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        try {
            String token = extractToken(httpRequest);
            if (token == null) {
                throw new BusinessException(401, "未提供认证token");
            }
            Long userId = userService.getUserIdByToken(token);
            if (userId == null) {
                throw new BusinessException(401, "token无效或已过期");
            }

            userGameAccountService.delete(userId, id);
            return CommonResponse.success("删除成功", null);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除账号失败", e);
            throw new BusinessException(500, "删除账号失败");
        }
    }

    /**
     * 内部方法：从请求头提取token
     */
    private String extractToken(HttpServletRequest request) {
        // 从Authorization header获取
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 从query参数获取（兼容性）
        String tokenParam = request.getParameter("token");
        if (tokenParam != null) {
            return tokenParam;
        }

        return null;
    }
}
