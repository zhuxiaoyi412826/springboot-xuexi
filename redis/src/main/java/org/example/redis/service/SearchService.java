package org.example.redis.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.redis.cache.MultiLevelCacheService;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.example.redis.vo.SongVO;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 搜索。
 *
 * <p>Redis 知识点：
 * <ul>
 *   <li>搜索结果缓存（热门关键词命中率极高）</li>
 *   <li>热搜榜：ZSet + ZINCRBY，天然按热度排序</li>
 *   <li>搜索历史：List（LPUSH + LTRIM）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final String CACHE_SEARCH = "search";

    private final SongService songService;
    private final MultiLevelCacheService cache;
    private final RedisService redis;
    private final ObjectMapper mapper;

    public List<SongVO> search(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String kw = keyword.trim();
        // 1) 热搜词计分
        redis.zIncrBy(RedisKeys.RANK_SEARCH, kw, 1);
        // 2) 搜索历史（登录用户）
        Long uid = songService.currentUserIdOrNull();
        if (uid != null) {
            redis.lPushAndTrim(String.format(RedisKeys.SEARCH_HISTORY, uid), kw, 10);
        }
        // 3) 结果缓存 2 分钟
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, SongVO.class);
        List<SongVO> result = cache.get(CACHE_SEARCH, kw, type, Duration.ofMinutes(2), () -> {
            var page = songService.page(1, limit, kw, null);
            return new ArrayList<>(page.getList() == null ? List.of() : page.getList());
        });
        return result == null ? List.of() : result;
    }

    /** 热搜榜（带分数） */
    public List<Map<String, Object>> hotSearch(int limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redis.zRevRangeWithScores(RedisKeys.RANK_SEARCH, 0, limit - 1);
        List<Map<String, Object>> list = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("keyword", t.getValue());
            m.put("score", t.getScore() == null ? 0 : t.getScore().longValue());
            list.add(m);
        }
        return list;
    }

    public List<String> history() {
        Long uid = songService.currentUserIdOrNull();
        if (uid == null) {
            return List.of();
        }
        return redis.lRange(String.format(RedisKeys.SEARCH_HISTORY, uid), 0, 9);
    }

    public void clearHistory() {
        Long uid = songService.currentUserIdOrNull();
        if (uid != null) {
            redis.delete(String.format(RedisKeys.SEARCH_HISTORY, uid));
        }
    }
}
