package org.example.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * 知识点：密码加密。
 *
 * <p>使用 DelegatingPasswordEncoder：密文形如 {@code {bcrypt}$2a$10$...}，
 * 前缀决定用哪个编码器校验，这样系统可以在不强制所有用户改密码的前提下升级算法。</p>
 *
 * <ul>
 *     <li>{bcrypt}：默认，BCrypt（自带随机盐）</li>
 *     <li>{pbkdf2}：PBKDF2（FIPS 友好）</li>
 *     <li>{argon2}：Argon2id（需要 BouncyCastle，目前公认更抗 GPU 破解）</li>
 * </ul>
 *
 * <p>注意：这里刻意没有注册 {noop}，生产禁止明文密码。</p>
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());

        // 第 1 个参数是「新密码用哪种算法加密」，校验时按密文前缀自动选择算法
        return new DelegatingPasswordEncoder("bcrypt", encoders);
    }
}
