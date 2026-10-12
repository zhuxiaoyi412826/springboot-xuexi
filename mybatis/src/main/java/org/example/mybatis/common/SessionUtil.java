package org.example.mybatis.common;

import jakarta.servlet.http.HttpSession;
import org.example.mybatis.entity.User;

/**
 * 会话工具：登录信息存取 + 权限校验（基于 HttpSession）
 */
public final class SessionUtil {

    private static final String KEY_USER = "LOGIN_USER";

    private SessionUtil() {
    }

    public static void login(HttpSession session, User user) {
        session.setAttribute(KEY_USER, user);
    }

    public static void logout(HttpSession session) {
        session.removeAttribute(KEY_USER);
    }

    public static User getLoginUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object obj = session.getAttribute(KEY_USER);
        return obj instanceof User u ? u : null;
    }

    /** 必须登录，否则抛异常 */
    public static User requireLogin(HttpSession session) {
        User user = getLoginUser(session);
        if (user == null) {
            throw new BizException(401, "请先登录");
        }
        return user;
    }

    /** 必须管理员 */
    public static User requireAdmin(HttpSession session) {
        User user = requireLogin(session);
        if (!"ADMIN".equals(user.getRole())) {
            throw new BizException(403, "无权限，仅管理员可操作");
        }
        return user;
    }
}
