package org.example.security.config;

import lombok.RequiredArgsConstructor;
import org.example.security.security.ApiKeyAuthenticationFilter;
import org.example.security.security.ApiKeyAuthenticationProvider;
import org.example.security.security.LoginFailureHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

/**
 * 第二条过滤器链：{@code /api/**} —— OAuth2 资源服务器（前后端分离 / 微服务常用）。
 *
 * <p>特点：无状态（不创建 session）、关闭 CSRF（无 cookie 就无 CSRF 风险）、
 * 校验 {@code Authorization: Bearer <JWT>}；另外插入了自定义的 API Key 过滤器，
 * 演示「同一个接口支持两种认证方式」。</p>
 */
@Configuration
@RequiredArgsConstructor
public class ApiSecurityConfig {

    private final JwtDecoder jwtDecoder;
    private final ApiKeyAuthenticationProvider apiKeyAuthenticationProvider;
    private final LoginFailureHandler loginFailureHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        ApiKeyAuthenticationFilter apiKeyFilter =
                new ApiKeyAuthenticationFilter(new ProviderManager(apiKeyAuthenticationProvider), loginFailureHandler);

        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        // 签发令牌与探活：探活要求已认证（可用 JWT 或 API Key）
                        .requestMatchers("/api/auth/token").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/videos").hasAuthority("video:write")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
                // 自定义过滤器插在 BearerToken 之前：先试 API Key，再试 JWT
                .addFilterBefore(apiKeyFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    /** 把 JWT 里的 roles / perms 声明还原成 Authorities */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));
            }
            List<String> perms = jwt.getClaimAsStringList("perms");
            if (perms != null) {
                perms.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
            }
            // 兼容标准 scope 声明
            String scope = jwt.getClaimAsString("scope");
            if (scope != null && !scope.isBlank()) {
                for (String s : scope.split(" ")) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
                }
            }
            return authorities;
        });
        return converter;
    }
}
