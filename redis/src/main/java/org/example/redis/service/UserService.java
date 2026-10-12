package org.example.redis.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.cache.MultiLevelCacheService;
import org.example.redis.common.BizException;
import org.example.redis.common.PageResult;
import org.example.redis.entity.User;
import org.example.redis.mapper.UserMapper;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.example.redis.vo.UserVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 用户与登录。
 *
 * <p>Redis 知识点：
 * <ul>
 *   <li>Sa-Token 的 token / session 由 Redis 持久化（sa-token-redis-jackson），多实例共享登录态</li>
 *   <li>在线用户列表用 Set 自己维护（{@code music:online:users}）</li>
 *   <li>用户资料、角色走 L1/L2/L3 三级缓存</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String CACHE_USER = "user";
    private static final String CACHE_ROLE = "userRole";

    private final UserMapper userMapper;
    private final MultiLevelCacheService cache;
    private final RedisService redis;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ==================== 登录 / 注册 ====================

    public UserVO login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(403, "账号已被禁用，请联系管理员");
        }

        // 1) Sa-Token 登录（token 会写进 Redis）
        StpUtil.login(user.getId());
        // 2) 把用户快照放进 Sa-Token 的 Session（同样在 Redis 里）
        //    注意：自定义类型必须先注册到 Sa-Token 的 JSON 白名单（见 RedisApplication 静态块），
        //    否则读 Session 时会报「无法反序列化的类型：org.example.redis.vo.UserVO」
        StpUtil.getSession().set("user", UserVO.from(user));
        // 3) 自己维护在线用户集合
        redis.sAdd(RedisKeys.ONLINE_USERS, String.valueOf(user.getId()));
        // 4) 发一条登录通知（Pub/Sub）
        redis.publish(RedisKeys.CHANNEL_NOTICE, "用户 " + username + " 登录了");

        UserVO vo = UserVO.from(user);
        vo.setRole(roleOf(user.getId()));
        return vo;
    }

    public void logout() {
        try {
            Object loginId = StpUtil.getLoginId();
            redis.sRem(RedisKeys.ONLINE_USERS, String.valueOf(loginId));
            cache.evict(CACHE_USER, String.valueOf(loginId));
            cache.evict(CACHE_ROLE, String.valueOf(loginId));
        } catch (Exception ignored) {
            // 未登录时直接忽略
        }
        StpUtil.logout();
    }

    public UserVO register(String username, String password, String nickname) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        // BCrypt 加密：同样明文每次密文都不同，且带随机盐
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname == null || nickname.isBlank() ? username : nickname);
        user.setRole("user");
        user.setStatus(1);
        user.setSignDays(0);
        userMapper.insert(user);
        return UserVO.from(user);
    }

    // ==================== 查询（带三级缓存） ====================

    public UserVO currentUser() {
        Long id = Long.parseLong(StpUtil.getLoginId().toString());
        return getCached(id);
    }

    public UserVO getCached(Long id) {
        return cache.get(CACHE_USER, String.valueOf(id), UserVO.class, Duration.ofMinutes(10),
                () -> {
                    User user = userMapper.selectById(id);
                    return user == null ? null : UserVO.from(user);
                });
    }

    /** 角色缓存：角色判断在每次鉴权都会走到，缓存收益最大 */
    public String roleOf(Long id) {
        String role = cache.get(CACHE_ROLE, String.valueOf(id), String.class, Duration.ofMinutes(30),
                () -> {
                    User user = userMapper.selectById(id);
                    return user == null ? null : user.getRole();
                });
        return role == null ? "user" : role;
    }

    public List<String> permissionOf(Long id) {
        return "admin".equals(roleOf(id))
                ? List.of("song:manage", "user:manage", "comment:manage", "cache:manage", "playlist:audit")
                : List.of("song:play", "song:like", "song:favorite", "comment:add", "playlist:create");
    }

    // ==================== 管理端 ====================

    public PageResult<UserVO> page(int page, int size, String keyword) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like(User::getUsername, keyword).or().like(User::getNickname, keyword);
        }
        qw.orderByDesc(User::getId);
        IPage<User> p = userMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p, UserVO::from);
    }

    /** 启用 / 禁用（禁用后要立刻踢下线，否则 token 还能用） */
    public void setStatus(Long id, int status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        cache.evict(CACHE_USER, String.valueOf(id));
        cache.evict(CACHE_ROLE, String.valueOf(id));
        if (status == 0) {
            StpUtil.kickout(id);
            redis.sRem(RedisKeys.ONLINE_USERS, String.valueOf(id));
        }
    }

    /** 踢下线 */
    public void kickout(Long id) {
        StpUtil.kickout(id);
        redis.sRem(RedisKeys.ONLINE_USERS, String.valueOf(id));
    }

    /** 封禁 N 秒（Sa-Token 的账号封禁，也是存在 Redis 里的） */
    public void ban(Long id, long seconds) {
        StpUtil.disable(id, seconds);
        StpUtil.kickout(id);
    }

    public void unban(Long id) {
        StpUtil.untieDisable(id);
    }

    /** 在线用户（Set） */
    public java.util.Set<String> onlineUsers() {
        return redis.sMembers(RedisKeys.ONLINE_USERS);
    }

    public long onlineCount() {
        return redis.sCard(RedisKeys.ONLINE_USERS);
    }
}
