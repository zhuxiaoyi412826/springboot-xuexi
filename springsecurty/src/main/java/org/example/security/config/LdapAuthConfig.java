package org.example.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

/**
 * 知识点：LDAP 目录服务认证（企业内网常见）。
 *
 * <p>只有当配置了 {@code spring.ldap.urls} 时才会启用，避免没有 LDAP 服务器时启动失败。
 * 启用方式（application.yaml 或启动参数）：
 * <pre>
 * spring:
 *   ldap:
 *     urls: ldap://127.0.0.1:389
 *     base: dc=example,dc=org
 *     username: cn=admin,dc=example,dc=org
 *     password: secret
 * app:
 *   ldap:
 *     user-dn-pattern: uid={0},ou=people
 *     group-search-base: ou=groups
 * </pre>
 * 该 Provider 会被 SecurityConfig 追加到 AuthenticationManager 中（与数据库账号并存）。</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "spring.ldap", name = "urls")
public class LdapAuthConfig {

    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(BaseLdapPathContextSource contextSource,
                                                                 org.springframework.core.env.Environment env) {
        String userDnPattern = env.getProperty("app.ldap.user-dn-pattern", "uid={0},ou=people");
        String groupSearchBase = env.getProperty("app.ldap.group-search-base", "ou=groups");

        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserDnPatterns(new String[]{userDnPattern});

        DefaultLdapAuthoritiesPopulator authoritiesPopulator =
                new DefaultLdapAuthoritiesPopulator(contextSource, groupSearchBase);
        authoritiesPopulator.setGroupRoleAttribute("cn");
        authoritiesPopulator.setRolePrefix("ROLE_");

        log.info("启用 LDAP 认证：userDnPattern={}, groupSearchBase={}", userDnPattern, groupSearchBase);
        return new LdapAuthenticationProvider(authenticator, authoritiesPopulator);
    }
}
