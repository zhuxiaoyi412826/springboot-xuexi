package org.example.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 知识点：Spring Security 同时支持 Servlet 与 WebFlux（响应式）两种栈。
 *
 * <p>本项目主站是 Servlet 栈；这里给出响应式栈的等价配置，默认关闭
 * （{@code app.webflux.enabled=false}），打开后可与 Servlet 配置共存对比学习：
 * <ul>
 *     <li>Servlet：SecurityFilterChain（HttpSecurity）</li>
 *     <li>WebFlux：SecurityWebFilterChain（ServerHttpSecurity）</li>
 * </ul>
 * 启动参数：{@code --app.webflux.enabled=true}</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "app.webflux", name = "enabled", havingValue = "true")
@EnableWebFluxSecurity
public class WebFluxSecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(ex -> ex
                        .pathMatchers("/reactive/public/**").permitAll()
                        .pathMatchers("/reactive/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated())
                .httpBasic(b -> {
                })
                .formLogin(f -> f.loginPage("/user/login.html"))
                .csrf(c -> c.disable())
                .build();
    }

    /** 响应式 Web 用的是 ReactiveUserDetailsService（返回 Mono<UserDetails>） */
    @Bean
    public ReactiveUserDetailsService reactiveUserDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.withUsername("reactive")
                .password(passwordEncoder.encode("123456"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
        return new MapReactiveUserDetailsService(user);
    }
}
