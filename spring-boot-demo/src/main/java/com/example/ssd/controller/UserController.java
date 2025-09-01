package com.example.ssd.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

//    @Autowired
//    private RateLimitService rateLimitService;
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    @Autowired
//    private IUserService userService;
//
//    @Autowired
//    private RedisUtil redisUtil;
////
////    @Autowired
////    private RedissonClient redissonClient;
//
//    private static final String PRODUCT_ID = "10001";
//
//    private static AtomicInteger value1 = new AtomicInteger(0);
//    private static AtomicInteger value2 = new AtomicInteger(0);
//    private static boolean flag = false;
//
//
//    @GetMapping("/volatile")
//    public ApiResponse<String> verify() throws InterruptedException {
//
//        Thread thread1 = new Thread(() -> {
//            value1.set(42);
//            value2.set(84);
//            flag = true;
//        });
//
//        Thread thread2 = new Thread(() -> {
//            while (!flag) {
//                // 等待 flag 被设置
//            }
//            System.out.println("Value1: " + value1.get() + ", Value2: " + value2.get());
//        });
//
//        thread1.start();
//        thread2.start();
//
//        thread1.join();
//        thread2.join();
//        return null;
//    }
//
//
//    @GetMapping("/log")
//    public ApiResponse<String> printLog() {
//        log.info("hello world !,info");
//        log.error("hello world !,error");
//        log.debug("hello world !,debug");
//        return ApiResponse.success("操作成功");
//    }
//
//    @GetMapping("/{id}")
//    public ApiResponse<User> getUser(@PathVariable("id") Long id) {
//        User user = userService.getById(id);
//        int a = 1 / 0;
//        return ApiResponse.success(user);
//    }
//
//    @GetMapping("/myendpoint")
//    public ApiResponse rateLimit() {
////        if (rateLimitService.isAllowed("customLimiter", 5)) {
////            return ApiResponse.success(null);
////        }
//        return ApiResponse.error(500);
//    }
//
//    @GetMapping("/second/kill")
//    public void deductStock() {
//        boolean lock = redisUtil.tryLock(PRODUCT_ID, 10, TimeUnit.SECONDS);
//        if (!lock) {
//            return;
//        }
//        // 获取库存
//        Integer stock = (Integer) redisTemplate.opsForValue().get("stock");
//        if (stock > 0) {
//            System.out.println("剩余库存：" + redisTemplate.opsForValue().decrement("stock"));
//        } else {
//            System.out.println("已经售罄！");
//        }
//        redisUtil.releaseLock(PRODUCT_ID);
//    }
//
//    @GetMapping("/seckill")
//    public String seckill() {
//        // 获取锁对象
//        RLock lock = redissonClient.getLock(PRODUCT_ID);
//        try {
//            // 加锁
//            boolean isLocked = lock.tryLock(10, 30, TimeUnit.SECONDS);
//            if (!isLocked) {
//                return "系统繁忙，请稍后重试";
//            }
//            // 扣减库存
//            Long stock = redisTemplate.opsForValue().decrement("stock:" + PRODUCT_ID);
//            if (stock < 0) {
//                return "商品已经售罄";
//            }
//            // 创建订单
//            redisTemplate.opsForValue().increment("order:" + PRODUCT_ID);
//            return "抢购成功";
//        } catch (Exception e) {
//            return "系统繁忙，请稍后重试";
//        } finally {
//            // 释放锁
//            if (lock != null && lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//    }
//
//    @GetMapping("/user/{id}")
//    public String user(@PathVariable("id") Long id) {
//        return userService.getUserName(id);
//    }

}
