package org.example.security.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.security.common.Result;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 「未登录/认证失败」的统一入口：
 * <ul>
 *     <li>页面请求 → 跳转自定义登录页 /user/login.html</li>
 *     <li>XHR / API 请求 → 返回 401 JSON（不跳页面）</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class DelegatingAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final LoginUrlAuthenticationEntryPoint pageEntryPoint =
            new LoginUrlAuthenticationEntryPoint("/user/login.html");

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, jakarta.servlet.ServletException {
        if (WebJson.wantJson(request)) {
            WebJson.write(response, objectMapper, 401, Result.unauthorized("未登录或登录已失效，请先登录"));
            return;
        }
        pageEntryPoint.commence(request, response, authException);
    }
}
