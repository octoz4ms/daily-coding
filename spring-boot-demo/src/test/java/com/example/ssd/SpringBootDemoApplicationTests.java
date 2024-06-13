package com.example.ssd;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.ssd.entity.User;
import com.example.ssd.service.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootTest
class SpringBootDemoApplicationTests {

    @Autowired
    private IUserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    void contextLoads() {
        ValueOperations opsForValue = redisTemplate.opsForValue();
        User user = new User();
        user.setId(1L);
        user.setName("zms");
        opsForValue.set("user1", user);
    }

}
