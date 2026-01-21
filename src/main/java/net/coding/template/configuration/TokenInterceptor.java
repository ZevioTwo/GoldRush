package net.coding.template.configuration;

import lombok.extern.slf4j.Slf4j;
import net.coding.template.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Resource
    private UserService userService;

    // 不需要token验证的路径
    private static final String[] EXCLUDE_PATHS = {
            "/api/user/login",
            "/api/user/register",
            "/error",
            "/swagger-ui.html",
            "/swagger-resources",
            "/v2/api-docs",
            "/webjars"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        // 检查是否在排除列表
        for (String excludePath : EXCLUDE_PATHS) {
            if (requestURI.startsWith(excludePath)) {
                return true;
            }
        }

        // 从header获取token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else {
            // 从参数获取（兼容）
            token = request.getParameter("token");
        }

        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"未提供认证token\"}");
            return false;
        }

        // 验证token
        Long userId = userService.getUserIdByToken(token);
        if (userId == null) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"token无效或已过期\"}");
            return false;
        }

        // 将userId存入request attribute，供后续使用
        request.setAttribute("userId", userId);

        return true;
    }
}
