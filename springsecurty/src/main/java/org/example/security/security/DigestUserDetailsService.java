package org.example.security.security;

import lombok.RequiredArgsConstructor;
import org.example.security.entity.User;
import org.example.security.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * HTTP Digest 认证专用的用户查询。
 *
 * <p>Digest 的校验公式里需要 A1 = MD5(username:realm:password)，而 BCrypt 不可逆，
 * 无法从密文反推 A1，所以用户表额外存了一份 digest_hash，这里把它当作「密码」交给
 * DigestAuthenticationFilter（配合 passwordAlreadyEncoded=true）。</p>
 *
 * <p><b>注意</b>：这里刻意<b>不</b>实现 {@link org.springframework.security.core.userdetails.UserDetailsService}。
 * 因为容器里一旦出现两个 UserDetailsService bean，Spring Security 会自动跳过
 * DaoAuthenticationProvider 的装配（不报错），导致表单登录报
 * “No AuthenticationProvider found for UsernamePasswordAuthenticationToken”。
 * 因此本类只提供方法，在 DigestSecurityConfig 里以 lambda 形式传给过滤器。</p>
 */
@Component
@RequiredArgsConstructor
public class DigestUserDetailsService {

    private final UserRepository userRepository;

    public UserDetails loadByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在：" + username));
        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getDigestHash() == null ? "" : user.getDigestHash(),
                user.getNickname(),
                user.getRoles(),
                Boolean.TRUE.equals(user.getEnabled()),
                Boolean.TRUE.equals(user.getAccountNonLocked()));
    }
}
