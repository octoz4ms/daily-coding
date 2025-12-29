package com.octo.eum.mq;

import com.octo.eum.config.RabbitMQConfig;
import com.octo.eum.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 用户事件消费者
 *
 * @author octo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    /**
     * 处理用户创建事件
     */
    @RabbitListener(queues = RabbitMQConfig.USER_CREATE_QUEUE)
    public void handleUserCreate(User user) {
        try {
            log.info("收到用户创建事件: userId={}, username={}", user.getId(), user.getUsername());
            // 这里可以做一些异步处理，比如：
            // 1. 发送欢迎邮件
            // 2. 初始化用户数据
            // 3. 同步到其他系统
            // 等等...
        } catch (Exception e) {
            log.error("处理用户创建事件失败: {}", e.getMessage(), e);
        }
    }
}

