package com.octo.rc.rabbitmq.listener;

import com.alibaba.fastjson.JSON;
import com.octo.rc.rabbitmq.entity.SeckillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀消息消费者
 * 
 * 演示队列限流场景下死信队列的使用
 */
@Component
@Slf4j
public class SeckillConsumer {

    /**
     * 处理秒杀请求
     * 模拟处理较慢（100ms），造成队列积压
     */
    @RabbitListener(queues = "seckill.queue")
    public void handleSeckill(String message) throws InterruptedException {
        SeckillMessage seckillMessage = JSON.parseObject(message, SeckillMessage.class);
        log.info("处理秒杀请求，用户: {}, 商品: {}", 
                seckillMessage.getUserId(), seckillMessage.getProductId());
        
        // 模拟业务处理耗时
        Thread.sleep(100);
        
        log.info("秒杀成功！用户: {}, 商品: {}", 
                seckillMessage.getUserId(), seckillMessage.getProductId());
    }

    /**
     * 处理被拒绝的秒杀请求（队列满导致溢出）
     * 
     * 这里可以：
     * 1. 通知用户"活动太火爆，请稍后再试"
     * 2. 记录统计数据
     */
    @RabbitListener(queues = "seckill.dlx.queue")
    public void handleRejectedSeckill(String message) {
        SeckillMessage seckillMessage = JSON.parseObject(message, SeckillMessage.class);
        log.warn("【秒杀失败-队列已满】用户: {}, 商品: {}, 请求时间: {}", 
                seckillMessage.getUserId(), 
                seckillMessage.getProductId(),
                seckillMessage.getRequestTime());
        
        // 可以通过 WebSocket 或其他方式通知用户
        notifyUser(seckillMessage.getUserId(), "活动太火爆了，请稍后再试！");
    }

    private void notifyUser(String userId, String message) {
        log.info("通知用户 {}: {}", userId, message);
        // 实际项目中可以通过 WebSocket、消息推送等方式通知用户
    }
}











