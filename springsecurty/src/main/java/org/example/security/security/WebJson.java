package org.example.security.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.security.common.Result;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 判断「这个请求该返回 JSON 还是跳页面」，并统一写 JSON 响应。
 *
 * <p>知识点：浏览器表单（页面）应跳转到登录页，而 XHR / API 应直接返回 401/403 JSON。</p>
 */
public final class WebJson {

    private WebJson() {
    }

    /** 是否期望 JSON 响应：XHR 头、Accept: application/json、或访问 /app、/api 前缀 */
    public static boolean wantJson(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/app/") || uri.startsWith("/api/") || uri.startsWith("/digest/") || uri.startsWith("/basic/")) {
            return true;
        }
        String xhr = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xhr)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    public static void write(HttpServletResponse response, ObjectMapper mapper, int status, Result<?> body)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(mapper.writeValueAsString(body));
    }
}
