package com.example.ssd.controller;


import com.example.ssd.entity.User;
import com.example.ssd.service.IUserService;
import com.example.ssd.service.RateLimitService;
import com.example.ssd.utils.ApiResponse;
import com.example.ssd.utils.RedisUtil;
import com.octo.cssb.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author zms
 * @since 2024-05-30
 */
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
