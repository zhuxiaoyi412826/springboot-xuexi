package org.example.redis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.redis.cache.IdGeneratorService;
import org.example.redis.cache.MultiLevelCacheService;
import org.example.redis.cache.RateLimiterService;
import org.example.redis.common.BizException;
import org.example.redis.common.PageResult;
import org.example.redis.entity.Comment;
import org.example.redis.entity.User;
import org.example.redis.mapper.CommentMapper;
import org.example.redis.mapper.UserMapper;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.example.redis.vo.CommentVO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 评论业务。
 *
 * <p>Redis 知识点：
 * <ul>
 *   <li>评论列表用短 TTL 缓存（变更频繁，缓存时间要短）</li>
 *   <li>发评论用令牌桶限流（Lua）+ 幂等防重复提交（SET NX）</li>
 *   <li>评论序号用 Redis INCR 生成分布式 ID</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final String CACHE_COMMENT = "comment";

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final MultiLevelCacheService cache;
    private final RedisService redis;
    private final RateLimiterService rateLimiter;
    private final IdGeneratorService idGenerator;
    private final ObjectMapper mapper;
    private final SongService songService;

    /** 歌曲评论列表（缓存 1 分钟） */
    public List<CommentVO> list(Long songId) {
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, CommentVO.class);
        return cache.get(CACHE_COMMENT, String.valueOf(songId), type, Duration.ofMinutes(1),
                () -> queryList(songId));
    }

    private List<CommentVO> queryList(Long songId) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getSongId, songId).eq(Comment::getStatus, 1)
                .orderByDesc(Comment::getCreateTime).last("limit 100"));
        return comments.stream().map(this::toVo).toList();
    }

    /**
     * 发评论：限流 + 幂等 + 分布式 id。
     *
     * @param idempotentKey 前端生成的唯一 key（同一次提交重复点击会被拦住）
     */
    public CommentVO add(Long songId, String content, String idempotentKey) {
        Long uid = songService.currentUserIdOrNull();
        if (uid == null) {
            throw new BizException(401, "请先登录");
        }
        if (content == null || content.isBlank()) {
            throw new BizException("评论内容不能为空");
        }
        // 1) 令牌桶限流：每用户 5 个令牌容量，每秒恢复 1 个
        if (!rateLimiter.allow("comment:" + uid, 5, 1, 1)) {
            throw new BizException(429, "评论太频繁了，喝口水再来～");
        }
        // 2) 幂等：同一个 key 10 秒内只处理一次
        if (idempotentKey != null && !idempotentKey.isBlank()) {
            String idemKey = String.format(RedisKeys.IDEMPOTENT, idempotentKey);
            if (!Boolean.TRUE.equals(redis.setIfAbsent(idemKey, "1", Duration.ofSeconds(10)))) {
                throw new BizException("请勿重复提交");
            }
        }

        Comment c = new Comment();
        c.setId(idGenerator.nextId("comment"));
        c.setSongId(songId);
        c.setUserId(uid);
        c.setContent(content);
        c.setLikeCount(0L);
        c.setStatus(1);
        commentMapper.insert(c);

        cache.evict(CACHE_COMMENT, String.valueOf(songId));
        return toVo(c);
    }

    /** 评论点赞（String INCR） */
    public long like(Long commentId) {
        Comment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw new BizException("评论不存在");
        }
        String key = String.format(RedisKeys.CNT_LIKE, "comment:" + commentId);
        redis.incr(key);
        c.setLikeCount((c.getLikeCount() == null ? 0 : c.getLikeCount()) + 1);
        commentMapper.updateById(c);
        cache.evict(CACHE_COMMENT, String.valueOf(c.getSongId()));
        return c.getLikeCount();
    }

    /** 删除评论：作者本人或管理员 */
    public void delete(Long commentId) {
        Long uid = songService.currentUserIdOrNull();
        Comment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw new BizException("评论不存在");
        }
        boolean isAdmin = uid != null && "admin".equals(roleOf(uid));
        if (!isAdmin && !c.getUserId().equals(uid)) {
            throw new BizException(403, "只能删除自己的评论");
        }
        commentMapper.deleteById(commentId);
        cache.evict(CACHE_COMMENT, String.valueOf(c.getSongId()));
    }

    /** 管理端：屏蔽/放开 */
    public void shield(Long commentId, int status) {
        Comment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw new BizException("评论不存在");
        }
        c.setStatus(status);
        commentMapper.updateById(c);
        cache.evict(CACHE_COMMENT, String.valueOf(c.getSongId()));
    }

    public PageResult<CommentVO> adminPage(int page, int size, String keyword) {
        LambdaQueryWrapper<Comment> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like(Comment::getContent, keyword);
        }
        qw.orderByDesc(Comment::getId);
        IPage<Comment> p = commentMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p, this::toVo);
    }

    private CommentVO toVo(Comment c) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setSongId(c.getSongId());
        vo.setUserId(c.getUserId());
        vo.setContent(c.getContent());
        vo.setLikeCount(c.getLikeCount());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
        User user = userMapper.selectById(c.getUserId());
        vo.setNickname(user == null ? "匿名" : (user.getNickname() == null ? user.getUsername() : user.getNickname()));
        vo.setAvatar(user == null ? null : user.getAvatar());
        return vo;
    }

    private String roleOf(Long uid) {
        User user = userMapper.selectById(uid);
        return user == null ? "user" : user.getRole();
    }
}
