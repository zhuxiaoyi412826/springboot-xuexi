package org.example.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import java.util.List;

/**
 * 知识点：HTTP 防火墙（拦截非法请求）。
 *
 * <p>StrictHttpFirewall 默认会拒绝：URL 中的编码斜杠(%2F)、编码分号、反斜杠、不可打印字符，
 * 以及 TRACE 这类危险方法，从源头挡住请求走私、路径穿越等攻击。</p>
 *
 * <p>这里显式声明允许的 HTTP 方法（默认已不含 TRACE），并放开 URL 编码的百分号
 * （搜索关键字里可能带 %）。</p>
 */
@Configuration
public class HttpFirewallConfig {

    @Bean
    public HttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowedHttpMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        firewall.setAllowUrlEncodedPercent(true);
        firewall.setAllowUrlEncodedSlash(false);   // 保持默认拒绝：防路径穿越
        firewall.setAllowSemicolon(false);         // 保持默认拒绝：防请求参数注入
        return firewall;
    }
}
