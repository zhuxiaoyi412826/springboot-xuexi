package org.example.security.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义 AuthenticationProvider：校验请求头里的 API Key。
 *
 * <p>知识点：Spring Security 的认证是「过滤器(Filter) → AuthenticationManager →
 * AuthenticationProvider」三段式，自己实现 Provider 就能接入任意认证方式
 * （短信验证码、API Key、签名、CAS、X509…）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    @Value("${app.api-key}")
    private String expectedApiKey;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationToken token = (ApiKeyAuthenticationToken) authentication;
        String apiKey = String.valueOf(token.getCredentials());
        if (!expectedApiKey.equals(apiKey)) {
            throw new org.springframework.security.authentication.BadCredentialsException("API Key 无效");
        }
        log.debug("API Key 认证通过");
        return new ApiKeyAuthenticationToken(
                "api-client",
                apiKey,
                List.of(new SimpleGrantedAuthority("ROLE_API"),
                        new SimpleGrantedAuthority("api:access"),
                        new SimpleGrantedAuthority("video:view")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
