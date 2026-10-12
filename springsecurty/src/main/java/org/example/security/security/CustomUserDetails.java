package org.example.security.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 自定义 UserDetails：把数据库里的「角色」翻译成 Spring Security 的「权限(Authority)」。
 *
 * <ul>
 *     <li>角色（Role）：ROLE_USER / ROLE_ADMIN / ROLE_VIP，用于 hasRole()</li>
 *     <li>权限（Authority/细粒度）：video:write、user:manage…，用于 hasAuthority()</li>
 * </ul>
 */
@Getter
@Setter
public class CustomUserDetails implements UserDetails, java.io.Serializable {

    /** 会话里保存的就是它，必须可序列化（Spring Session + Redis 场景） */
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final String password;
    private final String nickname;
    private final Set<String> roles;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<GrantedAuthority> authorities;

    public CustomUserDetails(Long userId, String username, String password, String nickname,
                             Set<String> roles, boolean enabled, boolean accountNonLocked) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.nickname = nickname == null ? username : nickname;
        this.roles = roles;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.authorities = buildAuthorities(roles);
    }

    /** 角色 → 角色 + 细粒度权限 */
    private static Collection<GrantedAuthority> buildAuthorities(Set<String> roles) {
        List<GrantedAuthority> list = new ArrayList<>();
        roles.forEach(r -> list.add(new SimpleGrantedAuthority(r)));

        if (roles.contains("ROLE_ADMIN")) {
            list.add(new SimpleGrantedAuthority("user:manage"));
            list.add(new SimpleGrantedAuthority("video:write"));
            list.add(new SimpleGrantedAuthority("video:delete"));
            list.add(new SimpleGrantedAuthority("comment:delete"));
            list.add(new SimpleGrantedAuthority("session:kick"));
        }
        if (roles.contains("ROLE_USER")) {
            list.add(new SimpleGrantedAuthority("video:view"));
            list.add(new SimpleGrantedAuthority("comment:write"));
            list.add(new SimpleGrantedAuthority("favorite:write"));
        }
        if (roles.contains("ROLE_VIP")) {
            list.add(new SimpleGrantedAuthority("video:download"));
        }
        return list;
    }

    /** 是否管理员（供 @PreAuthorize("@perm.isAdmin(authentication)") 使用） */
    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /** 账户是否过期（可扩展：按会员到期时间判断） */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    /** 凭证是否过期（可扩展：强制 90 天改一次密码） */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
