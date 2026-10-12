package org.example.redis.cache;

/** 缓存层级。 */
public enum CacheLevel {
    /** L1：进程内本地缓存（Caffeine），最快，但各实例不共享 */
    L1,
    /** L2：分布式缓存（Redis），跨实例共享 */
    L2,
    /** L3：数据源（MySQL），回源查询 */
    L3
}
