package com.octo.rc.rabbitmq.controller;

import com.octo.rc.rabbitmq.entity.MessageRecord;
import com.octo.rc.rabbitmq.service.IdempotentService;
import com.octo.rc.rabbitmq.service.MessageRecordService;
import com.octo.rc.rabbitmq.service.ReliableMessageProducer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 可靠消息测试控制器
 * 
 * 提供测试接口验证消息可靠性机制
 */
@RestController
@RequestMapping("/reliable")
@Slf4j
public class ReliableMessageController {

    @Autowired
    private ReliableMessageProducer reliableMessageProducer;

    @Autowired
    private MessageRecordService messageRecordService;

    @Autowired
    private IdempotentService idempotentService;

    /**
     * 发送可靠消息
     * 
     * @param request 请求参数
     * @return 消息ID
     */
    @PostMapping("/send")
    public Map<String, Object> sendMessage(@RequestBody SendMessageRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        String businessId = request.getBusinessId();
        if (businessId == null || businessId.isEmpty()) {
            businessId = UUID.randomUUID().toString().replace("-", "");
        }

        String messageId = reliableMessageProducer.sendReliableMessage(
                businessId,
                request.getBusinessType(),
                request.getData()
        );

        result.put("success", true);
        result.put("messageId", messageId);
        result.put("businessId", businessId);
        log.info("✅ 消息发送请求已接收，messageId: {}", messageId);

        return result;
    }

    /**
     * 批量发送消息（用于压力测试）
     * 
     * @param count 发送数量
     * @return 发送结果
     */
    @PostMapping("/send/batch")
    public Map<String, Object> sendBatchMessages(@RequestParam(defaultValue = "10") int count) {
        Map<String, Object> result = new HashMap<>();
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;

        for (int i = 0; i < count; i++) {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("index", i);
                data.put("content", "批量消息 #" + i);

                reliableMessageProducer.sendReliableMessage(
                        "batch-" + i,
                        "BATCH_TEST",
                        data
                );
                successCount++;
            } catch (Exception e) {
                log.error("批量发送失败，index: {}, error: {}", i, e.getMessage());
            }
        }

        long costTime = System.currentTimeMillis() - startTime;
        result.put("success", true);
        result.put("totalCount", count);
        result.put("successCount", successCount);
        result.put("costTimeMs", costTime);

        log.info("✅ 批量发送完成，总数: {}, 成功: {}, 耗时: {}ms", count, successCount, costTime);
        return result;
    }

    /**
     * 发送会失败的消息（用于测试死信队列）
     * 
     * @return 发送结果
     */
    @PostMapping("/send/error")
    public Map<String, Object> sendErrorMessage() {
        Map<String, Object> result = new HashMap<>();

        // businessId 以 "error" 开头会触发业务异常
        String businessId = "error-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> data = new HashMap<>();
        data.put("testType", "error");
        data.put("description", "这条消息会触发业务异常，进入死信队列");

        String messageId = reliableMessageProducer.sendReliableMessage(
                businessId,
                "ERROR_TEST",
                data
        );

        result.put("success", true);
        result.put("messageId", messageId);
        result.put("businessId", businessId);
        result.put("note", "该消息会触发业务异常，最终进入死信队列");

        return result;
    }

    /**
     * 发送可能失败的消息（用于测试重试机制）
     * 
     * @return 发送结果
     */
    @PostMapping("/send/random")
    public Map<String, Object> sendRandomErrorMessage() {
        Map<String, Object> result = new HashMap<>();

        // businessId 以 "random" 开头会随机触发异常
        String businessId = "random-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> data = new HashMap<>();
        data.put("testType", "random");
        data.put("description", "这条消息有50%概率处理失败，用于测试重试机制");

        String messageId = reliableMessageProducer.sendReliableMessage(
                businessId,
                "RANDOM_TEST",
                data
        );

        result.put("success", true);
        result.put("messageId", messageId);
        result.put("businessId", businessId);
        result.put("note", "该消息有50%概率处理失败，触发重试机制");

        return result;
    }

    /**
     * 查询消息记录状态
     * 
     * @param messageId 消息ID
     * @return 消息记录
     */
    @GetMapping("/record/{messageId}")
    public Map<String, Object> getMessageRecord(@PathVariable String messageId) {
        Map<String, Object> result = new HashMap<>();
        
        MessageRecord record = messageRecordService.getByMessageId(messageId);
        if (record == null) {
            result.put("success", false);
            result.put("message", "消息记录不存在");
            return result;
        }

        result.put("success", true);
        result.put("record", record);
        return result;
    }

    /**
     * 查询所有消息记录
     * 
     * @return 所有消息记录
     */
    @GetMapping("/records")
    public Map<String, Object> getAllRecords() {
        Map<String, Object> result = new HashMap<>();
        
        List<MessageRecord> records = messageRecordService.getAllRecords();
        result.put("success", true);
        result.put("total", records.size());
        result.put("records", records);

        return result;
    }

    /**
     * 获取系统状态
     * 
     * @return 系统状态
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>();
        
        List<MessageRecord> records = messageRecordService.getAllRecords();
        
        long pendingCount = records.stream()
                .filter(r -> r.getStatus() == MessageRecord.STATUS_PENDING).count();
        long sendingCount = records.stream()
                .filter(r -> r.getStatus() == MessageRecord.STATUS_SENDING).count();
        long successCount = records.stream()
                .filter(r -> r.getStatus() == MessageRecord.STATUS_SUCCESS).count();
        long failedCount = records.stream()
                .filter(r -> r.getStatus() == MessageRecord.STATUS_FAILED).count();

        result.put("totalMessages", records.size());
        result.put("pendingMessages", pendingCount);
        result.put("sendingMessages", sendingCount);
        result.put("successMessages", successCount);
        result.put("failedMessages", failedCount);
        result.put("consumedMessageCount", idempotentService.getConsumedCount());

        return result;
    }

    /**
     * 发送消息请求体
     */
    @Data
    public static class SendMessageRequest {
        private String businessId;
        private String businessType = "DEFAULT";
        private Object data;
    }
}

