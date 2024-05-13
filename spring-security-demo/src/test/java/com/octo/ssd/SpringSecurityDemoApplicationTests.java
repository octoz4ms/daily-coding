package com.octo.ssd;

import com.octo.ssd.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class SpringSecurityDemoApplicationTests {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
        String encode = passwordEncoder.encode("1234");
        System.out.println(encode);
        boolean result = passwordEncoder.matches("1234", "$2a$10$8vdHj765W9UL8KbD0KQAFe4N0oTH36XX9Z58s3f9MfqKQXwxfUrGu");
        System.out.println(result);
    }

    @Test
    void contextLoads2() {
        String token = JwtUtils.generateToken("zms");
        System.out.println(token);
    }

}
