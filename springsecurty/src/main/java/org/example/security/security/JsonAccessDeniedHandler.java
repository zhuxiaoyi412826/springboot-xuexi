package org.example.security.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.common.Result;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 「已登录但权限不足」的处理：页面跳 403 页，XHR 返回 403 JSON。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        log.warn("拒绝访问：uri={} user={} 原因={}", request.getRequestURI(),
                auth == null ? "匿名" : auth.getName(), accessDeniedException.getMessage());

        if (WebJson.wantJson(request)) {
            WebJson.write(response, objectMapper, 403, Result.forbidden("权限不足：" + accessDeniedException.getMessage()));
            return;
        }
        redirectStrategy.sendRedirect(request, response, "/user/403.html");
    }
}
