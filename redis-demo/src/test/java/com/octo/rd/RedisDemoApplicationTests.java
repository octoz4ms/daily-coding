package com.octo.rd;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RedisDemoApplicationTests {

    @Autowired
    private RBloomFilter<String> usernameBloomFilter;

    @Autowired
    private RBloomFilter<String> productBloomFilter;

    @Test
    void addUser() {
        usernameBloomFilter.add("zms");
    }

    @Test
    void isExistUser() {
        boolean zms = usernameBloomFilter.contains("zms");
        boolean zh = usernameBloomFilter.contains("zh");
        System.out.println(zms);
        System.out.println(zh);
    }

    @Test
    void addProduct() {
        productBloomFilter.add("cup");
    }

    @Test
    void isExistProduct() {
        boolean cup = productBloomFilter.contains("cup");
        boolean tea = productBloomFilter.contains("tea");
        System.out.println(cup);
        System.out.println(tea);
    }
}
