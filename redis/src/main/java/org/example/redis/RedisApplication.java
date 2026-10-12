package org.example.redis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 在线听歌网站启动类。
 *
 * <p>技术栈：Spring Boot 4 + MyBatis-Plus + Lombok + Knife4j + Sa-Token + MySQL + Redis。
 * 项目有两条主线：
 * <ol>
 *   <li>完整的听歌业务（歌曲/歌单/评论/收藏/排行榜/搜索 + 用户端/管理端）</li>
 *   <li>Redis 学习：String/Hash/List/Set/ZSet/Bitmap/HyperLogLog/Geo/Stream/PubSub、
 *       分布式锁、Lua 限流、延迟队列、布隆过滤器、Pipeline、以及 L1(Caffeine) + L2(Redis) + L3(MySQL) 多级缓存</li>
 * </ol>
 */
@SpringBootApplication
@MapperScan("org.example.redis.mapper")
@org.springframework.scheduling.annotation.EnableScheduling
public class RedisApplication {

    /**
     * Sa-Token 的 JSON 反序列化白名单（必须在 Sa-Token 初始化之前注册，所以放静态块）。
     *
     * <p>原因：sa-token-redis-jackson 存 Session / token 时会开启 Jackson 的
     * {@code activateDefaultTyping}（JSON 里带 {@code @class} 字段），反序列化时
     * 只允许白名单里的类型，否则抛出「无法反序列化的类型：xxx，请先将其注册到 JSON 全局类型白名单」。
     * 我们把存进 SaSession 的 UserVO 注册进去即可；以后还有自定义类型要存，照着加一行。
     */
    static {
        cn.dev33.satoken.strategy.SaJsonStrategy.instance.registerAllowType(org.example.redis.vo.UserVO.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(RedisApplication.class, args);
    }
}
