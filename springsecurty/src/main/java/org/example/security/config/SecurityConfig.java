package org.example.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.security.AuditLogoutHandler;
import org.example.security.security.CustomUserDetailsService;
import org.example.security.security.DelegatingAuthenticationEntryPoint;
import org.example.security.security.JsonAccessDeniedHandler;
import org.example.security.security.LoginFailureHandler;
import org.example.security.security.LoginSuccessHandler;
import org.example.security.security.LogoutSuccessHandlerImpl;
import org.example.security.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.sql.DataSource;
import java.util.List;

/**
 * 主过滤器链（浏览器 + 页面 + /app/** AJAX）。
 *
 * <p>这是本项目的核心，覆盖了绝大部分知识点：
 * URL 授权、表单登录、HTTP Basic、OAuth2/OIDC 登录、记住我、匿名访问、
 * 会话固定攻击防护、并发会话控制、CSRF、安全响应头、退出登录、异常处理、X509、CORS。</p>
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final LogoutSuccessHandlerImpl logoutSuccessHandler;
    private final DelegatingAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final AuditLogoutHandler auditLogoutHandler;
    private final SessionRegistry sessionRegistry;
    private final DataSource dataSource;
    private final ObjectProvider<LdapAuthenticationProvider> ldapAuthenticationProvider;
    private final ObjectProvider<ClientRegistrationRepository> oauth2ClientRepositoryProvider;

    /** 未配置第三方登录时为 null，此时不启用 oauth2Login */
    private final ClientRegistrationRepository oauth2ClientRepository;

    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                          LoginSuccessHandler loginSuccessHandler,
                          LoginFailureHandler loginFailureHandler,
                          LogoutSuccessHandlerImpl logoutSuccessHandler,
                          DelegatingAuthenticationEntryPoint authenticationEntryPoint,
                          JsonAccessDeniedHandler accessDeniedHandler,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                          AuditLogoutHandler auditLogoutHandler,
                          SessionRegistry sessionRegistry,
                          DataSource dataSource,
                          ObjectProvider<LdapAuthenticationProvider> ldapAuthenticationProvider,
                          ObjectProvider<ClientRegistrationRepository> oauth2ClientRepositoryProvider) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.loginSuccessHandler = loginSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.auditLogoutHandler = auditLogoutHandler;
        this.sessionRegistry = sessionRegistry;
        this.dataSource = dataSource;
        this.ldapAuthenticationProvider = ldapAuthenticationProvider;
        this.oauth2ClientRepositoryProvider = oauth2ClientRepositoryProvider;
        this.oauth2ClientRepository = oauth2ClientRepositoryProvider.getIfAvailable();
    }

    /** 页面与 AJAX（/app/**）走这条链；作为兜底链，序号最大 */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE - 10)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        // 显式注册用户名密码认证器：查库(UserDetailsService) + 校验密文(PasswordEncoder)
        // 不显式注册时，若容器里 UserDetailsService 不唯一，Spring Security 会静默跳过自动装配，
        // 结果就是登录时报 “No AuthenticationProvider found for UsernamePasswordAuthenticationToken”。
        http.authenticationProvider(daoAuthenticationProvider());

        // 知识点：可插拔的认证方式——配了 LDAP 就把它加入 ProviderManager
        ldapAuthenticationProvider.ifAvailable(p -> http.authenticationProvider(p));

        http
                /* ---------------- 1. 授权：URL 请求级别权限 ---------------- */
                .authorizeHttpRequests(auth -> auth
                        // 静态资源与公开页面：匿名可访问
                        .requestMatchers(
                                "/", "/index.html", "/favicon.ico", "/error",
                                "/css/**", "/js/**", "/img/**", "/media/**", "/uploads/**",
                                "/user/login.html", "/user/register.html", "/user/403.html",
                                "/user/home.html", "/user/detail.html",
                                "/oauth2/**", "/login/oauth2/**",
                                // 浏览/搜索视频、看评论：匿名可访问
                                "/app/videos/list", "/app/videos/hot", "/app/videos/{id}",
                                "/app/comments/list", "/app/categories",
                                // 登录、注册、获取 CSRF Token
                                "/app/auth/**"
                        ).permitAll()
                        // 角色控制：个人中心必须登录
                        .requestMatchers("/user/me.html", "/app/me/**").hasRole("USER")
                        // 角色控制：管理端页面
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 细粒度权限（Authority）控制：也可以用 hasAuthority 替代 hasRole
                        .requestMatchers("/app/admin/**").hasAuthority("user:manage")
                        // VIP 才能下载
                        .requestMatchers("/app/videos/*/download").hasAuthority("video:download")
                        .requestMatchers("/app/comments/add").hasAuthority("comment:write")
                        .requestMatchers("/app/videos/*/like", "/app/videos/*/favorite").hasAuthority("favorite:write")
                        .anyRequest().authenticated())

                /* ---------------- 2. 认证：表单登录 ---------------- */
                .formLogin(form -> form
                        .loginPage("/user/login.html")           // 自定义登录页（static 下）
                        .loginProcessingUrl("/login")            // 表单提交地址
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll())

                /* ---------------- 3. 认证：HTTP Basic（API / curl 常用） ---------------- */
                .httpBasic(basic -> basic.realmName("VideoSite"))

                /* ---------------- 5. 记住我（持久化令牌，存数据库） ---------------- */
                .rememberMe(remember -> remember
                        .key("video-site-remember-me-key")
                        .tokenValiditySeconds(14 * 24 * 3600)
                        .rememberMeParameter("remember-me")
                        .tokenRepository(persistentTokenRepository(dataSource))
                        .userDetailsService(userDetailsService))

                /* ---------------- 6. 退出登录 ---------------- */
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler(auditLogoutHandler)
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)              // 清除 session
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "remember-me") // 清除 cookie
                        .permitAll())

                /* ---------------- 7. 匿名访问 ---------------- */
                .anonymous(anon -> anon
                        .principal("guest")
                        .authorities("ROLE_ANONYMOUS"))

                /* ---------------- 8. 会话管理 ---------------- */
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                        // 会话固定攻击防护：登录成功后更换 sessionId
                        .sessionFixation(fix -> fix.changeSessionId())
                        .invalidSessionUrl("/user/login.html?invalid=1")
                        // 并发登录控制：同一账号最多 2 个会话，超过则踢掉最早的
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(2)
                                .maxSessionsPreventsLogin(false)   // true=拒绝新登录，false=踢掉旧会话
                                .expiredUrl("/user/login.html?expired=1")
                                .sessionRegistry(sessionRegistry)))

                /* ---------------- 9. CSRF ---------------- */
                // 浏览器表单/页面启用（SPA 从 XSRF-TOKEN cookie 读 token 放到 X-XSRF-TOKEN 头）
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))

                /* ---------------- 10. 安全响应头 ---------------- */
                .headers(headers -> headers
                        // 防点击劫持
                        .frameOptions(frame -> frame.deny())
                        // CSP：防 XSS / 资源劫持
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "script-src 'self'; "
                                        + "style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data:; "
                                        + "media-src 'self' https: blob:; "
                                        + "connect-src 'self'; "
                                        + "frame-ancestors 'none'"))
                        // HSTS：强制 HTTPS
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(ref -> ref
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // 关闭不需要的浏览器能力（摄像头/麦克风/定位）
                        .permissionsPolicyHeader(perm -> perm.policy(
                                "geolocation=(), microphone=(), camera=(), payment=()")))

                /* ---------------- 11. 异常处理：未登录 / 无权限 ---------------- */
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                /* ---------------- 12. X509 客户端证书认证（有证书才生效） ---------------- */
                .x509(x509 -> x509.subjectPrincipalRegex("CN=(.*?)(?:,|$)"))

                /* ---------------- 13. CORS（前后端分离时需要） ---------------- */
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        /* ---------------- 4. 认证：OAuth2 / OIDC 第三方登录 ---------------- */
        // 必须「方法级」判空：只要调用 http.oauth2Login(...)，Spring Security 就会要求
        // ClientRegistrationRepository 存在，未配置第三方登录时会直接启动失败。
        if (oauth2ClientRepository != null) {
            http.oauth2Login(oauth -> oauth
                    .loginPage("/user/login.html")
                    .clientRegistrationRepository(oauth2ClientRepository)
                    .successHandler(oAuth2LoginSuccessHandler)
                    .failureHandler(loginFailureHandler));
        } else {
            log.info("未配置 OAuth2 客户端（app.oauth2.github.client-id 为空），跳过 oauth2Login");
        }

        return http.build();
    }

    /** 用户名 + 密码的认证器（表单登录、HTTP Basic 都用它） */
    @Bean
    public org.springframework.security.authentication.dao.DaoAuthenticationProvider daoAuthenticationProvider() {
        org.springframework.security.authentication.dao.DaoAuthenticationProvider provider =
                new org.springframework.security.authentication.dao.DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * 全局 AuthenticationManager（由 UserDetailsService + PasswordEncoder 自动装配出 DaoAuthenticationProvider）。
     * OAuth2 令牌接口 /api/auth/token 会用到它。
     */
    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** 记住我：持久化令牌存 MySQL（persistent_logins 表） */
    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        // 表结构已在 db/schema.sql 中创建，这里不自动建表
        return repository;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5500"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION, "X-XSRF-TOKEN", "X-API-Key"));
        config.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION, "X-Request-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(1800L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/app/**", config);
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
