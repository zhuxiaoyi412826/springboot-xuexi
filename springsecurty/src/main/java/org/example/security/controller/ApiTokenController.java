package org.example.security.controller;

import lombok.RequiredArgsConstructor;
import org.example.security.common.Result;
import org.example.security.security.JwtTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 知识点：OAuth2 资源服务器的「另一半」——令牌签发（模拟认证中心）。
 *
 * <pre>
 * curl -X POST http://localhost:8084/api/auth/token \
 *      -H "Content-Type: application/json" \
 *      -d '{"username":"admin","password":"admin123"}'
 * </pre>
 * 返回的 access_token 再以 {@code Authorization: Bearer xxx} 访问 /api/**。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ApiTokenController {

    private final AuthenticationManager authenticationManager;
    private final org.example.security.security.CustomUserDetailsService userDetailsService;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/token")
    public Result<Map<String, Object>> token(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        UserDetails details = userDetailsService.loadUserByUsername(username);
        Long userId = details instanceof org.example.security.security.CustomUserDetails cu ? cu.getUserId() : null;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("access_token", jwtTokenService.issue(details, userId));
        data.put("token_type", "Bearer");
        data.put("expires_in", jwtTokenService.ttlSeconds());
        data.put("authorities", details.getAuthorities().stream()
                .map(a -> a.getAuthority()).toList());
        return Result.ok(data);
    }
}
