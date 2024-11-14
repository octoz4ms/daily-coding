package com.example.ssd.controller;


import com.example.ssd.entity.User;
import com.example.ssd.service.IUserService;
import com.example.ssd.service.RateLimitService;
import com.example.ssd.utils.ApiResponse;
import com.example.ssd.utils.RedisUtil;
import com.octo.cssb.HelloService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author zms
 * @since 2024-05-30
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private IUserService userService;

    @Autowired
    private HelloService helloService;

    @Autowired
    private RedisUtil redisUtil;

    private static final String PRODUCT_ID = "10001";

    private static AtomicInteger value1 = new AtomicInteger(0);
    private static AtomicInteger value2 = new AtomicInteger(0);
    private static boolean flag = false;



    @GetMapping("/volatile")
    public ApiResponse<String> verify() throws InterruptedException {

            Thread thread1 = new Thread(() -> {
                value1.set(42);
                value2.set(84);
                flag = true;
            });

            Thread thread2 = new Thread(() -> {
                while (!flag) {
                    // 等待 flag 被设置
                }
                System.out.println("Value1: " + value1.get() + ", Value2: " + value2.get());
            });

            thread1.start();
            thread2.start();

            thread1.join();
            thread2.join();
        return null;
    }





    @GetMapping("/log")
    public ApiResponse<String> printLog() {
        log.info("hello world !,info");
        log.error("hello world !,error");
        log.debug("hello world !,debug");
        return ApiResponse.success("操作成功");
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getUser(@PathVariable("id") Long id) {
        User user = userService.getById(id);
        int a = 1 / 0;
        return ApiResponse.success(user);
    }

    @GetMapping("/myendpoint")
    public ApiResponse rateLimit() {
        if (rateLimitService.isAllowed("customLimiter", 5)) return ApiResponse.success(null);
        return ApiResponse.error(500);
    }

    @GetMapping("/second/kill")
    public void deductStock() {
        boolean lock = redisUtil.tryLock(PRODUCT_ID, 10, TimeUnit.SECONDS);
        if (!lock) return;
        // 获取库存
        Integer stock = (Integer) redisTemplate.opsForValue().get("stock");
        if (stock > 0) System.out.println("剩余库存：" + redisTemplate.opsForValue().decrement("stock"));
        else
            System.out.println("已经售罄！");
        redisUtil.releaseLock(PRODUCT_ID);
        // 扣减库存
//        Boolean result = redisTemplate.opsForValue().setIfAbsent(PRODUCT_ID, 1);
//        if (!result) return;
//        // 获取库存
//        Integer stock = (Integer) redisTemplate.opsForValue().get("stock");
//        // 扣减库存
//        if (stock > 0) {
//            Long remain = redisTemplate.opsForValue().decrement("stock");
//            System.out.println("剩余库存：" + remain);
//        } else System.out.println("已售罄！");
//
//        redisTemplate.delete(PRODUCT_ID);
    }
}
