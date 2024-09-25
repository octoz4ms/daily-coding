package com.example.ssd;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class SpringBootDemoApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Test
    void contextLoads() {
        RBloomFilter<Object> myFirstFilter = redissonClient.getBloomFilter("myFirstFilter");
        myFirstFilter.tryInit(1000, 0.01);

//        myFirstFilter.add("zms");
//        myFirstFilter.add("zh");
//        myFirstFilter.add("wff");


        boolean zz = myFirstFilter.contains("zz");
        boolean zms = myFirstFilter.contains("zms");
    }

    @Test
    void test() {
        Object a = 0;
        System.out.println(a);
    }

}
