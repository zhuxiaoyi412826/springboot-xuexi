package org.example.security.config;

import lombok.RequiredArgsConstructor;
import org.example.security.security.DigestUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.DigestAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationFilter;

/**
 * 第三条过滤器链：{@code /digest/**} —— HTTP Digest 摘要认证。
 *
 * <p>Basic 会把密码 base64 明文传输；Digest 用「用户名+realm+密码」的 MD5 摘要 + 随机数(nonce)
 * 完成认证，避免口令直接出现在网络上（配合 HTTPS 更安全）。
 * 浏览器/curl 都支持，可用 {@code curl --digest -u admin:admin123 http://localhost:8084/digest/me} 验证。</p>
 *
 * <p>注意：Digest 需要服务端能拿到 A1=MD5(user:realm:password)，因此用户表额外存了 digest_hash。</p>
 */
@Configuration
@RequiredArgsConstructor
public class DigestSecurityConfig {

    private final DigestUserDetailsService digestUserDetailsService;

    @Bean
    @Order(2)
    public SecurityFilterChain digestSecurityFilterChain(HttpSecurity http) throws Exception {
        DigestAuthenticationEntryPoint entryPoint = new DigestAuthenticationEntryPoint();
        entryPoint.setRealmName("VideoSite");
        entryPoint.setKey("video-site-digest-key");
        entryPoint.setNonceValiditySeconds(300);

        DigestAuthenticationFilter digestFilter = new DigestAuthenticationFilter();
        // 用方法引用而非第二个 UserDetailsService bean，避免破坏 DaoAuthenticationProvider 的自动装配
        digestFilter.setUserDetailsService(digestUserDetailsService::loadByUsername);
        digestFilter.setAuthenticationEntryPoint(entryPoint);
        digestFilter.setPasswordAlreadyEncoded(true);   // 库里存的就是 A1 摘要
        digestFilter.setCreateAuthenticatedToken(true); // 直接产生已认证的令牌

        http
                .securityMatcher("/digest/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("USER"))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
                .addFilterBefore(digestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
