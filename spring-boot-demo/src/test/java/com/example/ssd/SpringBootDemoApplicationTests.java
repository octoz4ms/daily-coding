package com.example.ssd;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.ssd.entity.User;
import com.example.ssd.service.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringBootDemoApplicationTests {

    @Autowired
    private IUserService userService;
    @Test
    void contextLoads() {
//        User one = userService.getOne(Wrappers.lambdaQuery(User.class).eq(User::getId, 1));
//        System.out.println(one);
    }

}
