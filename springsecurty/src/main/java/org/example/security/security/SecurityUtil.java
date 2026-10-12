package org.example.security.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 获取当前登录信息的工具（Service 里经常要用「当前用户」做数据权限判断） */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /** 当前用户 id，匿名返回 null */
    public static Long currentUserId() {
        Authentication auth = auth();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails user) {
            return user.getUserId();
        }
        return null;
    }

    public static String currentUsername() {
        Authentication auth = auth();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        if (auth.getPrincipal() instanceof CustomUserDetails user) {
            return user.getUsername();
        }
        return auth.getName();
    }

    public static boolean isAuthenticated() {
        Authentication auth = auth();
        return auth != null && auth.isAuthenticated()
                && !(auth.getPrincipal() instanceof String);
    }

    public static boolean isAdmin() {
        Authentication auth = auth();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public static boolean hasAuthority(String authority) {
        Authentication auth = auth();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
