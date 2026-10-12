package org.example.redis.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 多级缓存参数（application.yaml 的 music.cache.*）。 */
@Data
@Component
@ConfigurationProperties(prefix = "music.cache")
public class CacheProperties {

    /** L1：Caffeine 本地缓存开关 */
    private boolean l1Enabled = true;
    /** L2：Redis 分布式缓存开关 */
    private boolean l2Enabled = true;
    /** L3：MySQL 回源开关（关闭后缓存未命中就直接返回 null，用于演示缓存层作用） */
    private boolean l3Enabled = true;

    private long l1TtlSeconds = 60;
    private long l2TtlSeconds = 600;
    /** 过期时间随机抖动，防止同时失效造成雪崩 */
    private long l2JitterSeconds = 120;
    /** 空值缓存时间，防穿透 */
    private long nullTtlSeconds = 60;
    /** 热点 key 逻辑过期时间 */
    private long logicalExpireSeconds = 300;
    /** 缓存重建时互斥锁最长等待时间 */
    private long lockWaitSeconds = 5;
    /** 是否启用布隆过滤器（防穿透） */
    private boolean bloomEnabled = true;
    /** 是否统计各级命中率 */
    private boolean statsEnabled = true;
}
