package com.octo.eum;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密测试
 *
 * @author octo
 */
class PasswordEncoderTest {

    @Test
    void generateEncodedPassword() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        
        String rawPassword = "admin123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        System.out.println("==========================================");
        System.out.println("原始密码: " + rawPassword);
        System.out.println("加密后密码: " + encodedPassword);
        System.out.println("==========================================");
        
        // 验证密码是否匹配
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        System.out.println("密码验证结果: " + matches);
        
        assert matches : "密码验证失败";
    }
}

