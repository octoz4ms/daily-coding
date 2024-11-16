package com.example.ssd;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringBootDemoApplicationTests {

    @Autowired
    private RedissonClient redissonClient;

    @Test
    public void test() {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter("BF");
        bloomFilter.tryInit(1000, 0.1);
        System.out.println(bloomFilter.count());
    }

    @Test
    public void addUser() {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter("BF");
        bloomFilter.add("zms");
    }

    @Test
    public void containUser() {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter("BF");
        boolean zms = bloomFilter.contains("zms");
        boolean zh = bloomFilter.contains("zh");
        System.out.println(zms);
        System.out.println(zh);
    }

//    @Autowired
//    private RedissonClient redissonClient;
//
//    @Autowired
//    private RBloomFilter<String> userRegisterBloomFilter;
//
//    @Autowired
//    private RBloomFilter<String> ageRegisterBloomFilter;
//
//
//    @Test
//    void testBloomFilter() {
////        userRegisterCachePenetrationBloomFilter.add("zms");
////        System.out.println(userRegisterBloomFilter.contains("zms"));
////        System.out.println(userRegisterBloomFilter.count());
//        System.out.println(ageRegisterBloomFilter.count());
//    }

}
