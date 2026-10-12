package org.example.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 基础配置。
 *
 * <p>说明：本项目统一使用 {@link org.springframework.data.redis.core.StringRedisTemplate}
 * （key/value 都是 String），对象统一由 Jackson 转 JSON 后再写入，好处是：
 * <ul>
 *   <li>避免 JDK 序列化带来的跨语言不可读、无法在 redis-cli 里直接查看的问题</li>
 *   <li>缓存/会话场景下不会出现 NotSerializableException</li>
 * </ul>
 */
@Configuration
public class RedisConfig {

    /**
     * 缓存序列化用的 ObjectMapper。
     * 必须注册 JavaTimeModule，否则 LocalDateTime 序列化会直接报错。
     */
    @Bean
    public ObjectMapper objectMapper() {
        return com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    /** Pub/Sub 监听容器：缓存失效广播、站内通知都挂在它上面 */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        return container;
    }
}
