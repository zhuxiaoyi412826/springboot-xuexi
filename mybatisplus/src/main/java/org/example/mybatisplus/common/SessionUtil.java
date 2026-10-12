package org.example.mybatisplus.common;

import jakarta.servlet.http.HttpSession;
import org.example.mybatisplus.entity.User;

/**
 * 登录会话工具：登录成功后把用户放进 HttpSession。
 */
public final class SessionUtil {

    public static final String LOGIN_USER = "LOGIN_USER";

    private SessionUtil() {
    }

    public static void setLoginUser(HttpSession session, User user) {
        session.setAttribute(LOGIN_USER, user);
    }

    public static User getLoginUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object obj = session.getAttribute(LOGIN_USER);
        return obj instanceof User user ? user : null;
    }

    /** 必须登录，否则抛异常 */
    public static User requireLogin(HttpSession session) {
        User user = getLoginUser(session);
        if (user == null) {
            throw new BizException("请先登录");
        }
        return user;
    }

    /** 必须是管理员，否则抛异常 */
    public static User requireAdmin(HttpSession session) {
        User user = requireLogin(session);
        if (!User.ROLE_ADMIN.equals(user.getRole())) {
            throw new BizException("无权限，仅管理员可操作");
        }
        return user;
    }
}
