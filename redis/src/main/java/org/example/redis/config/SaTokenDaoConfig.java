package org.example.redis.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoForRedisTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Sa-Token 的持久化层（会话/令牌存到 Redis）。
 *
 * <p>为什么要自己覆盖一个方法？
 * Sa-Token 自带的 Redis DAO 在「改 value 但保留剩余 TTL」时用的是
 * {@code SET key value KEEPTTL}，而 <b>KEEPTTL 是 Redis 6.0 才加的选项</b>，
 * 本机 Redis 是 5.0，登录后写令牌会直接报 {@code Error in execution}。
 * 这里改成经典写法：先读剩余 TTL → SET 新值 → 再把 TTL 补回去（Redis 2.x 就支持）。
 *
 * <p>只要注入一个 {@link SaTokenDao} 类型的 Bean，Sa-Token 就会用它，
 * 不再走 starter 的默认实现。
 */
@Configuration
public class SaTokenDaoConfig {

    /**
     * 必须加 @Primary：starter 自己也会注册一个 SaTokenDaoForRedisTemplate，
     * 不加的话 Sa-Token 的注入点会拿到两个候选 Bean 而报错。
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public SaTokenDao saTokenDao(RedisConnectionFactory connectionFactory) {
        return new SaTokenDaoForRedisTemplate() {
            {
                // 父类需要显式绑定连接工厂
                init(connectionFactory);
            }

            /** 改 value，保留原来的过期时间（父类用 SET ... KEEPTTL，Redis 5 不支持） */
            @Override
            public void setStringAndKeepTTL(String key, String value) {
                setKeepTtl(key, value);
            }

            /** 同上：update 语义也是「保留 TTL 改值」 */
            @Override
            public void update(String key, String value) {
                setKeepTtl(key, value);
            }

            private void setKeepTtl(String key, String value) {
                long ttl = getTimeout(key);
                if (ttl > 0) {
                    set(key, value, ttl);
                } else {
                    // -1 永不过期 / -2 key 不存在：直接写，不带过期时间
                    set(key, value, NEVER_EXPIRE);
                }
            }
        };
    }
}
