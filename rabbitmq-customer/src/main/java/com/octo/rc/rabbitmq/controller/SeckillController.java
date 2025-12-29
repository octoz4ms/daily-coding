package com.octo.rc.rabbitmq.controller;

import com.alibaba.fastjson.JSON;
import com.octo.rc.rabbitmq.entity.SeckillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 秒杀控制器
 * 
 * 测试队列限流：
 * 1. 批量发送请求，观察队列满后消息转入死信队列
 * 
 * 测试命令（循环发送200个请求）：
 * for i in {1..200}; do curl -X POST "http://localhost:8080/seckill/request?userId=user$i&productId=P001&activityId=A001"; done
 */
@RestController
@RequestMapping("/seckill")
@Slf4j
public class SeckillController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发起秒杀请求
     */
    @PostMapping("/request")
    public String seckillRequest(
            @RequestParam String userId,
            @RequestParam String productId,
            @RequestParam String activityId) {
        
        SeckillMessage message = SeckillMessage.builder()
                .requestId(UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .productId(productId)
                .activityId(activityId)
                .requestTime(LocalDateTime.now())
                .build();

        String messageJson = JSON.toJSONString(message);
        
        try {
            rabbitTemplate.convertAndSend(
                    "seckill.exchange",
                    "seckill.routing.key",
                    messageJson
            );
            log.info("秒杀请求已提交，用户: {}", userId);
            return "秒杀请求已提交，请等待结果通知";
        } catch (Exception e) {
            log.error("秒杀请求失败: {}", e.getMessage());
            return "系统繁忙，请稍后再试";
        }
    }

    /**
     * 批量发送秒杀请求（用于测试限流）
     */
    @PostMapping("/batch")
    public String batchSeckill(
            @RequestParam String productId,
            @RequestParam String activityId,
            @RequestParam(defaultValue = "200") int count) {
        
        for (int i = 0; i < count; i++) {
            SeckillMessage message = SeckillMessage.builder()
                    .requestId(UUID.randomUUID().toString().replace("-", ""))
                    .userId("user_" + i)
                    .productId(productId)
                    .activityId(activityId)
                    .requestTime(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    "seckill.exchange",
                    "seckill.routing.key",
                    JSON.toJSONString(message)
            );
        }
        
        log.info("批量发送秒杀请求完成，数量: {}", count);
        return String.format("已发送 %d 个秒杀请求，队列最大长度为100，超出的将转入死信队列", count);
    }
}




