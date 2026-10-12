package org.example.security.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.common.Result;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 退出登录：清除 session、删除 remember-me cookie，页面请求跳登录页，XHR 返回 JSON。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {

    private final ObjectMapper objectMapper;
    private final DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (authentication != null) {
            log.info("退出登录：{}", authentication.getName());
        }
        // remember-me cookie 已在 SecurityConfig 的 deleteCookies(...) 中清除
        if (WebJson.wantJson(request)) {
            WebJson.write(response, objectMapper, 200, Result.ok("已退出登录"));
            return;
        }
        redirectStrategy.sendRedirect(request, response, "/user/login.html?logout=1");
    }
}
