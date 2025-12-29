package com.octo.rc.rabbitmq.task;

import com.octo.rc.rabbitmq.entity.MessageRecord;
import com.octo.rc.rabbitmq.service.IdempotentService;
import com.octo.rc.rabbitmq.service.MessageRecordService;
import com.octo.rc.rabbitmq.service.ReliableMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消息重发定时任务
 * 
 * 扫描本地消息表，对发送失败或未确认的消息进行重发
 * 保证消息的最终一致性
 */
@Component
@Slf4j
public class MessageResendTask {

    @Autowired
    private MessageRecordService messageRecordService;

    @Autowired
    private ReliableMessageProducer reliableMessageProducer;

    @Autowired
    private IdempotentService idempotentService;

    /**
     * 消息重发任务
     * 
     * 每 30 秒执行一次，扫描待重发的消息
     */
    @Scheduled(fixedDelay = 30000)
    public void resendFailedMessages() {
        log.debug("🔄 开始扫描待重发消息...");

        List<MessageRecord> messagesToResend = messageRecordService.getMessagesToResend();

        if (messagesToResend.isEmpty()) {
            log.debug("✅ 没有需要重发的消息");
            return;
        }

        log.info("📋 发现 {} 条待重发消息", messagesToResend.size());

        for (MessageRecord record : messagesToResend) {
            try {
                log.info("🔄 重发消息，messageId: {}, businessId: {}, retryCount: {}/{}", 
                        record.getMessageId(), 
                        record.getBusinessId(),
                        record.getRetryCount(),
                        record.getMaxRetryCount());

                // 检查是否超过最大重试次数
                if (record.getRetryCount() >= record.getMaxRetryCount()) {
                    log.error("❌ 消息重试次数超限，标记为失败，messageId: {}", record.getMessageId());
                    messageRecordService.markAsFailed(record.getMessageId(), "超过最大重试次数");
                    continue;
                }

                // 增加重试次数
                messageRecordService.incrementRetryCount(record.getMessageId());

                // 重发消息
                reliableMessageProducer.resendMessage(record);

            } catch (Exception e) {
                log.error("❌ 消息重发异常，messageId: {}, error: {}", 
                        record.getMessageId(), e.getMessage(), e);
            }
        }

        log.info("✅ 消息重发任务完成");
    }

    /**
     * 清理过期消费记录
     * 
     * 每小时执行一次
     */
    @Scheduled(fixedDelay = 3600000)
    public void cleanExpiredRecords() {
        log.info("🧹 开始清理过期消费记录...");
        idempotentService.cleanExpiredRecords();
        log.info("✅ 清理过期消费记录完成");
    }

    /**
     * 清理成功的消息记录
     * 
     * 每天执行一次，清理 7 天前成功的消息
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanSuccessMessages() {
        log.info("🧹 开始清理过期成功消息...");
        messageRecordService.cleanSuccessMessages(7);
        log.info("✅ 清理过期成功消息完成");
    }
}

