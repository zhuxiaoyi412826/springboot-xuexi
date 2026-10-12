package org.example.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.common.BizException;
import org.example.security.dto.PageResult;
import org.example.security.dto.UserVO;
import org.example.security.entity.User;
import org.example.security.repository.UserRepository;
import org.example.security.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.security.RolesAllowed;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 用户业务。
 *
 * <p>演示四种「方法级权限」注解：
 * <ul>
 *     <li>{@code @PreAuthorize}：Spring Security 主力注解（SpEL，最灵活）</li>
 *     <li>{@code @Secured}：老式角色注解（需要 securedEnabled=true）</li>
 *     <li>{@code @RolesAllowed}：JSR-250 标准注解（需要 jsr250Enabled=true）</li>
 *     <li>{@code @PostFilter}：见 VideoService</li>
 * </ul>
 */
@Slf4j
@Service
public class UserService {

    /** Digest 认证的 realm，必须与 DigestSecurityConfig 里的一致 */
    @Value("${app.digest.realm:VideoSite}")
    private String digestRealm;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    /**
     * 容器里有两个 UserDetailsService（主用户查询 + Digest 专用），
     * 这里用 @Qualifier 明确指定主的那一个。
     */
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @org.springframework.beans.factory.annotation.Qualifier("customUserDetailsService")
                       UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    /** 注册：密码 BCrypt 加密，并额外算一份 Digest 用的 A1 摘要 */
    @Transactional
    public User register(String username, String password, String nickname) {
        if (userRepository.existsByUsername(username)) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setDigestHash(digestA1(username, password));
        user.setNickname(nickname == null || nickname.isBlank() ? username : nickname);
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setLoginFailCount(0);
        user.setSource("local");
        user.setCreateTime(LocalDateTime.now());
        user.setRoles(Set.of("ROLE_USER"));
        return userRepository.save(user);
    }

    public UserVO profile() {
        Long uid = SecurityUtil.currentUserId();
        if (uid == null) {
            throw new BizException(401, "未登录");
        }
        return UserVO.from(userRepository.findById(uid).orElseThrow(() -> new BizException("用户不存在")));
    }

    @Transactional
    public void updateProfile(String nickname, String email, String phone, String avatar) {
        Long uid = SecurityUtil.currentUserId();
        User user = userRepository.findById(uid).orElseThrow(() -> new BizException("用户不存在"));
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        userRepository.save(user);
    }

    /** 修改密码：必须校验旧密码，同时更新 digest_hash */
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        Long uid = SecurityUtil.currentUserId();
        User user = userRepository.findById(uid).orElseThrow(() -> new BizException("用户不存在"));
        var details = userDetailsService.loadUserByUsername(user.getUsername());
        if (!passwordEncoder.matches(oldPassword, details.getPassword())) {
            throw new BizException("旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setDigestHash(digestA1(user.getUsername(), newPassword));
        userRepository.save(user);
        log.info("用户 {} 修改了密码", user.getUsername());
    }

    /* ==================== 管理端（方法级权限演示） ==================== */

    @PreAuthorize("hasRole('ADMIN')")
    public PageResult<UserVO> adminList(String keyword, int page, int size) {
        var pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(size, 50), Sort.by(Sort.Direction.DESC, "id"));
        List<User> list = userRepository.search(keyword, pageable);
        long total = userRepository.countByKeyword(keyword);
        return PageResult.of(list.stream().map(UserVO::from).toList(), total, page, size);
    }

    /** 启用 / 禁用账号 */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void setEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        if (id.equals(SecurityUtil.currentUserId()) && !enabled) {
            throw new BizException("不能禁用自己");
        }
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    /** 分配角色 */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void setRoles(Long id, Set<String> roles) {
        User user = userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        user.setRoles(roles);
        userRepository.save(user);
    }

    /** 重置密码（@Secured 写法） */
    @Secured("ROLE_ADMIN")
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setDigestHash(digestA1(user.getUsername(), newPassword));
        user.setAccountNonLocked(true);
        user.setLockedUntil(null);
        user.setLoginFailCount(0);
        userRepository.save(user);
    }

    /** 解锁账号 */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void unlock(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        user.setAccountNonLocked(true);
        user.setLockedUntil(null);
        user.setLoginFailCount(0);
        userRepository.save(user);
    }

    /** 删除用户（JSR-250 @RolesAllowed 写法） */
    @RolesAllowed("ROLE_ADMIN")
    @Transactional
    public void remove(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BizException("用户不存在");
        }
        userRepository.deleteById(id);
    }

    /** A1 = MD5(username:realm:password)，HTTP Digest 认证用 */
    private String digestA1(String username, String password) {
        try {
            String raw = username + ":" + digestRealm + ":" + password;
            byte[] bytes = MessageDigest.getInstance("MD5").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("计算 Digest A1 失败", e);
        }
    }
}
