package org.example.security.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败计数（防暴力破解）+ 账号锁定。
 *
 * <p>知识点：Redis 在安全场景的典型用法——分布式共享的失败计数。
 * Redis 不可用时自动降级为本地 Map，保证登录功能不受影响。</p>
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final String PREFIX = "security:login:fail:";

    private final StringRedisTemplate redis;
    private final Map<String, Integer> localFallback = new ConcurrentHashMap<>();

    public LoginAttemptService(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redis = redisProvider.getIfAvailable();
        log.info("登录失败计数存储：{}", redis == null ? "本地内存（Redis 不可用）" : "Redis");
    }

    /** 记录一次失败，返回当前连续失败次数 */
    public long loginFailed(String username) {
        if (redis != null) {
            try {
                String key = PREFIX + username;
                Long count = redis.opsForValue().increment(key);
                redis.expire(key, Duration.ofMinutes(30));
                return count == null ? 1 : count;
            } catch (Exception e) {
                log.warn("Redis 不可用，降级为本地计数：{}", e.getMessage());
            }
        }
        return localFallback.merge(username, 1, Integer::sum);
    }

    /** 登录成功：清空计数 */
    public void loginSucceeded(String username) {
        if (redis != null) {
            try {
                redis.delete(PREFIX + username);
            } catch (Exception e) {
                log.warn("Redis 清理失败：{}", e.getMessage());
            }
        }
        localFallback.remove(username);
    }

    public long current(String username) {
        if (redis != null) {
            try {
                String v = redis.opsForValue().get(PREFIX + username);
                return v == null ? 0 : Long.parseLong(v);
            } catch (Exception ignored) {
            }
        }
        return localFallback.getOrDefault(username, 0);
    }
}
