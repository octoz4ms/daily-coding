package com.example.ssd;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.ssd.entity.User;
import com.example.ssd.service.IUserService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootTest
class SpringBootDemoApplicationTests {

    @Autowired
    private IUserService userService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    public void addElementToBloomFilter(String element) {
        RBloomFilter<Object> myBloomFilter = redissonClient.getBloomFilter("myBloomFilter");
        myBloomFilter.tryInit(1000, 0.1);
        myBloomFilter.add(element);
    }

    public boolean mightContainElement(String element) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("myBloomFilter");
        return bloomFilter.contains(element);
    }

    @Test
    void test() {
        addElementToBloomFilter("zms");
        addElementToBloomFilter("wff");

        boolean zms = mightContainElement("zms");
        System.out.println(zms);
        boolean zh = mightContainElement("zh");
        System.out.println(zh);
    }



}
