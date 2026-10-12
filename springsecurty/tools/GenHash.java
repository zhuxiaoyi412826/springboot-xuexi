import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 离线生成种子用户的密码哈希（只在开发时用一次，不参与打包） */
public class GenHash {
    public static void main(String[] args) throws Exception {
        BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
        System.out.println("BCRYPT_admin123=" + bc.encode("admin123"));
        System.out.println("BCRYPT_123456=" + bc.encode("123456"));
        System.out.println("BCRYPT_vip888=" + bc.encode("vip888"));

        Argon2PasswordEncoder argon2 = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        System.out.println("ARGON2_123456=" + argon2.encode("123456"));

        Pbkdf2PasswordEncoder pbkdf2 = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        System.out.println("PBKDF2_123456=" + pbkdf2.encode("123456"));

        // Digest 认证需要 A1 = MD5(username:realm:password)
        a1("admin", "admin123");
        a1("zhangsan", "123456");
        a1("lisi", "123456");
        a1("wangwu", "vip888");
    }

    static void a1(String u, String p) throws Exception {
        String raw = u + ":VideoSite:" + p;
        byte[] d = MessageDigest.getInstance("MD5").digest(raw.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b));
        System.out.println("A1_" + u + "=" + sb);
    }
}
