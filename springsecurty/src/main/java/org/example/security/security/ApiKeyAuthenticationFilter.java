package org.example.security.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 自定义过滤器：从请求头 {@code X-API-Key} 取凭证并交给 AuthenticationManager。
 *
 * <p>知识点：可自定义过滤器并插入到过滤器链的指定位置
 * （{@code http.addFilterBefore(filter, BearerTokenAuthenticationFilter.class)}）。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private final AuthenticationManager authenticationManager;
    private final AuthenticationFailureHandler failureHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER);
        if (StringUtils.hasText(apiKey) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                var result = authenticationManager.authenticate(new ApiKeyAuthenticationToken(apiKey));
                SecurityContextHolder.getContext().setAuthentication(result);
                log.debug("请求 {} 使用 API Key 认证", request.getRequestURI());
            } catch (AuthenticationException e) {
                SecurityContextHolder.clearContext();
                failureHandler.onAuthenticationFailure(request, response, e);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
