import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class CheckHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder e = new BCryptPasswordEncoder();
        String[] hashes = {
                "$2a$10$sSfA2oLx2qWRShvPXU3Bz.tuXLqHEFd6O//oI3AcvEIbPVyGIXZgu",
                "$2a$10$P4UzFdJ2B7T0odwMxXOUpu04JuD.g2aKo8byStZ3X1YrduyLcto2K",
                "$2a$10$OdbGqimWp5D9fIRSAY29s.M62ffG12/65oxaC3KF9jsAGX/bsJ2/y"
        };
        String[] pwds = {"admin123", "123456", "vip888"};
        for (int i = 0; i < hashes.length; i++) {
            System.out.println("hash[" + i + "] matches " + pwds[i] + " = " + e.matches(pwds[i], hashes[i]));
        }
        // 交叉验证：admin123 是否真的对应第一个 hash
        System.out.println("admin123 vs hash1 = " + e.matches("admin123", hashes[1]));
        System.out.println("123456 vs hash0 = " + e.matches("123456", hashes[0]));
    }
}
