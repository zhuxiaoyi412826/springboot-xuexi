package org.example.mvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.example.mvc.resolver.CurrentUserArgumentResolver;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录拦截器：只拦截 /mvc/secure/**，演示 include/exclude 路径配置与「直接响应（不进 Controller）」。
 */
@Slf4j
public class LoginInterceptor implements HandlerInterceptor, Ordered {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        Object user = session == null ? null : session.getAttribute(CurrentUserArgumentResolver.SESSION_KEY);
        if (user != null) {
            return true;
        }
        log.info("[LoginInterceptor] 未登录，拦截 {}", request.getRequestURI());
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录，请先登录\"}");
            return false;
        }
        response.sendRedirect(request.getContextPath() + "/pages/session.html");
        return false;
    }

    @Override
    public int getOrder() {
        return 2;
    }

    /** 仅为演示：统一的响应写出方法 */
    @SuppressWarnings("unused")
    private void writeJson(HttpServletResponse response, String json) throws Exception {
        response.getWriter().write(new String(json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    }
}
