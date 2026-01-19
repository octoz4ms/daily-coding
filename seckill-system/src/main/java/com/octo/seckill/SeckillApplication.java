package com.octo.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 秒杀系统启动类
 * 
 * 核心技术点：
 * 1. Redis缓存预热 - 活动开始前将库存加载到Redis
 * 2. 分布式锁 - Redisson实现，防止并发问题
 * 3. 消息队列削峰 - RabbitMQ异步处理订单
 * 4. 限流降级 - 令牌桶算法 + 注解式限流
 * 5. 库存超卖解决方案 - Redis预扣减 + 数据库乐观锁
 * 
 * @author octo
 */
@SpringBootApplication
@MapperScan("com.octo.seckill.mapper")
@EnableScheduling
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}

