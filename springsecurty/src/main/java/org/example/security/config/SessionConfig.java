package org.example.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;

/**
 * 知识点：会话管理。
 *
 * <ul>
 *     <li>{@link SessionRegistry}：登记所有在线会话，用于并发登录控制与「强制下线」</li>
 *     <li>{@link HttpSessionEventPublisher}：让容器销毁会话时同步通知 SessionRegistry（否则过期会话不会被清理）</li>
 *     <li>如果引入了 Spring Session（Redis），则改用 SpringSessionBackedSessionRegistry，
 *         这样并发控制与强制下线在「多实例部署」下也生效（会话共享）</li>
 * </ul>
 */
@Slf4j
@Configuration
public class SessionConfig {

    @Bean
    public SessionRegistry sessionRegistry(ObjectProvider<FindByIndexNameSessionRepository<?>> repositoryProvider) {
        FindByIndexNameSessionRepository<?> repository = repositoryProvider.getIfAvailable();
        if (repository != null) {
            log.info("会话注册表：SpringSessionBackedSessionRegistry（会话存储=Redis，支持多实例共享）");
            return new SpringSessionBackedSessionRegistry<>(repository);
        }
        log.info("会话注册表：SessionRegistryImpl（会话存储=本机内存）");
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
