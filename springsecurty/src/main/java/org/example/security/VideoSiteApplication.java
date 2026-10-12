package org.example.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * 在线视频浏览网站（Spring Security 学习项目）。
 *
 * <p>{@code @EnableMethodSecurity}：开启方法级安全。
 * <ul>
 *     <li>prePostEnabled=true（默认）：@PreAuthorize / @PostAuthorize / @PreFilter / @PostFilter</li>
 *     <li>securedEnabled=true：@Secured("ROLE_ADMIN")</li>
 *     <li>jsr250Enabled=true：@RolesAllowed("ROLE_ADMIN")（JSR-250 标准注解）</li>
 * </ul>
 * 具体用法见 service 包。</p>
 */
@SpringBootApplication
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class VideoSiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoSiteApplication.class, args);
    }
}
