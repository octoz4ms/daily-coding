package com.octo.eum.mq;

import com.octo.eum.config.RabbitMQConfig;
import com.octo.eum.entity.LoginLog;
import com.octo.eum.mapper.LoginLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 登录日志消费者
 *
 * @author octo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginLogConsumer {

    private final LoginLogMapper loginLogMapper;

    /**
     * 消费登录日志消息
     */
    @RabbitListener(queues = RabbitMQConfig.LOGIN_LOG_QUEUE)
    public void handleLoginLog(LoginLog loginLog) {
        try {
            loginLogMapper.insert(loginLog);
            log.debug("登录日志保存成功: user={}, type={}, status={}",
                    loginLog.getUsername(), loginLog.getType(), loginLog.getStatus());
        } catch (Exception e) {
            log.error("保存登录日志失败: {}", e.getMessage(), e);
            // 这里可以加入重试逻辑或死信队列处理
        }
    }
}

