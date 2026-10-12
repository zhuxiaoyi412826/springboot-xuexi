package org.example.mvc.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.mvc.common.Result;
import org.example.mvc.model.User;
import org.example.mvc.resolver.CurrentUserArgumentResolver;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 会话与 Cookie：HttpSession 登录/登出、@SessionAttributes（见 WizardController）、
 * RedirectAttributes + FlashMap、Cookie 读写删、@RequestAttribute。
 */
@Controller
@RequestMapping("/mvc/session")
public class SessionCookieController {

    /** 登录：写入 session，并用 Flash 属性带一次性消息重定向 */
    @PostMapping("/login")
    public String login(@RequestParam String username, HttpSession session, RedirectAttributes ra) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setNickname(username + "的昵称");
        session.setAttribute(CurrentUserArgumentResolver.SESSION_KEY, user);
        ra.addFlashAttribute("flashMsg", "登录成功：" + username + "（Flash 属性只可被读取一次）");
        return "redirect:/mvc/session/me";
    }

    /** 读取登录信息 + 读取 Flash 属性 */
    @GetMapping("/me")
    @ResponseBody
    public Result me(HttpServletRequest request, HttpSession session) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("loginUser", session.getAttribute(CurrentUserArgumentResolver.SESSION_KEY));
        data.put("sessionId", session.getId());
        Map<String, ?> flash = RequestContextUtils.getInputFlashMap(request);
        data.put("flashMsg", flash == null ? "无（Flash 已被消费或不存在）" : flash.get("flashMsg"));
        data.put("siteName", "由 @ControllerAdvice 的 @ModelAttribute 注入（仅视图渲染时可见）");
        return Result.ok(data);
    }

    /** 登出：销毁 session */
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        session.invalidate();
        ra.addFlashAttribute("flashMsg", "已退出登录");
        return "redirect:/mvc/session/me";
    }

    /** 写 Cookie */
    @GetMapping("/cookie/set")
    @ResponseBody
    public Result setCookie(HttpServletResponse response) {
        String value = UUID.randomUUID().toString().substring(0, 8);
        Cookie cookie = new Cookie("sid", value);
        cookie.setPath("/");
        cookie.setMaxAge(3600);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return Result.ok(Map.of("sid", value, "maxAge", 3600));
    }

    /** 读 Cookie（@CookieValue） */
    @GetMapping("/cookie/read")
    @ResponseBody
    public Result readCookie(@CookieValue(value = "sid", defaultValue = "无") String sid) {
        return Result.ok(Map.of("sid", sid));
    }

    /** 删 Cookie（maxAge=0） */
    @GetMapping("/cookie/delete")
    @ResponseBody
    public Result deleteCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("sid", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return Result.ok("Cookie 已删除");
    }

    /** @RequestAttribute：读取 RequestIdFilter 写入的属性 */
    @GetMapping("/attr")
    @ResponseBody
    public Result attr(@RequestAttribute("requestId") String requestId) {
        return Result.ok(Map.of("requestId", requestId));
    }
}
