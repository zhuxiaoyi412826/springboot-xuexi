package org.example.security.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.security.common.Result;
import org.example.security.dto.AuthStateVO;
import org.example.security.entity.User;
import org.example.security.security.CustomUserDetails;
import org.example.security.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 认证相关（登录状态、注册、CSRF Token）。
 */
@RestController
@RequestMapping("/app/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** 当前登录状态（匿名也能访问，返回 authenticated=false） */
    @GetMapping("/state")
    public Result<AuthStateVO> state(@CurrentSecurityContext SecurityContext context) {
        Authentication auth = context.getAuthentication();
        AuthStateVO vo = new AuthStateVO();
        if (auth == null) {
            return Result.ok(vo);
        }
        boolean anonymous = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ANONYMOUS".equals(a.getAuthority()));
        vo.setAuthenticated(!anonymous && auth.isAuthenticated()
                && !(auth.getPrincipal() instanceof String));
        if (vo.isAuthenticated()) {
            vo.setUsername(auth.getName());
            List<String> authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).toList();
            vo.setAuthorities(authorities);
            vo.setRoles(authorities.stream().filter(a -> a.startsWith("ROLE_")).toList());
            if (auth.getPrincipal() instanceof CustomUserDetails user) {
                vo.setUserId(user.getUserId());
                vo.setNickname(user.getNickname());
            }
        }
        return Result.ok(vo);
    }

    /** 注册 */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        User user = userService.register(body.get("username"), body.get("password"), body.get("nickname"));
        return Result.ok(Map.of("id", user.getId(), "username", user.getUsername()));
    }

    /**
     * 取 CSRF Token（配合 CookieCsrfTokenRepository：前端从 XSRF-TOKEN cookie 读取，
     * 或请求本接口拿 token 后放到 X-XSRF-TOKEN 请求头）。
     */
    @GetMapping("/csrf")
    public Result<Map<String, String>> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            return Result.fail(404, "未生成 CSRF Token");
        }
        return Result.ok(Map.of(
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName(),
                "token", token.getToken()));
    }
}
