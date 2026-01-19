package com.octo.seckill.mq;

import com.octo.seckill.config.RabbitMQConfig;
import com.octo.seckill.dto.SeckillMessage;
import com.octo.seckill.entity.SeckillOrder;
import com.octo.seckill.mapper.SeckillActivityMapper;
import com.octo.seckill.mapper.SeckillOrderMapper;
import com.octo.seckill.service.StockCacheService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * 秒杀消息消费者
 * 
 * 面试要点：消费者如何保证消息不丢失？
 * 
 * 1. 手动ACK模式
 *    - 消费成功后手动确认
 *    - 消费失败可选择重试或拒绝
 * 
 * 2. 幂等性保证
 *    - 通过唯一索引（user_id + activity_id）防止重复消费
 *    - 先查询再插入
 * 
 * 3. 异常处理
 *    - 业务异常：拒绝消息，进入死信队列
 *    - 系统异常：重新入队，等待重试
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillMessageConsumer {

    private final SeckillOrderMapper orderMapper;
    private final SeckillActivityMapper activityMapper;
    private final StockCacheService stockCacheService;

    /**
     * 消费秒杀消息，创建订单
     */
    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleSeckillMessage(SeckillMessage message, 
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到秒杀消息: userId={}, activityId={}, orderNo={}", 
                message.getUserId(), message.getActivityId(), message.getOrderNo());
        
        try {
            // 1. 幂等性检查 - 是否已创建订单
            int count = orderMapper.countByUserAndActivity(message.getUserId(), message.getActivityId());
            if (count > 0) {
                log.warn("重复消费，订单已存在: userId={}, activityId={}", 
                        message.getUserId(), message.getActivityId());
                // 恢复Redis库存
                stockCacheService.restoreStock(message.getActivityId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 数据库扣减库存（乐观锁）
            int updated = activityMapper.deductStock(message.getActivityId());
            if (updated == 0) {
                log.warn("数据库库存不足: activityId={}", message.getActivityId());
                // 恢复Redis库存
                stockCacheService.restoreStock(message.getActivityId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 3. 创建订单
            SeckillOrder order = new SeckillOrder();
            order.setOrderNo(message.getOrderNo());
            order.setUserId(message.getUserId());
            order.setActivityId(message.getActivityId());
            order.setProductId(message.getProductId());
            order.setSeckillPrice(message.getSeckillPrice());
            order.setStatus(0); // 待支付

            orderMapper.insert(order);
            
            log.info("秒杀订单创建成功: orderNo={}, userId={}", message.getOrderNo(), message.getUserId());

            // 4. 手动ACK确认
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("秒杀消息处理异常: {}", e.getMessage(), e);
            
            // 恢复Redis库存
            stockCacheService.restoreStock(message.getActivityId());
            
            // 拒绝消息，不重新入队（进入死信队列）
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 死信队列消费者 - 处理失败的消息
     */
    @RabbitListener(queues = RabbitMQConfig.SECKILL_DLX_QUEUE)
    public void handleDeadLetter(SeckillMessage message, Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("处理死信消息: userId={}, activityId={}, orderNo={}", 
                message.getUserId(), message.getActivityId(), message.getOrderNo());
        
        // 这里可以：
        // 1. 记录到数据库，人工处理
        // 2. 发送告警通知
        // 3. 尝试最后一次补偿
        
        channel.basicAck(deliveryTag, false);
    }
}

