package com.octo.shorturl.service;

import com.octo.shorturl.entity.IdSegment;
import com.octo.shorturl.mapper.IdSegmentMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 短码生成器
 * 
 * 面试重点：如何生成不重复的短码？
 * 
 * 方案对比：
 * 
 * 1. 自增ID + 62进制转换（本项目采用）
 *    - 优点：简单可靠，永不重复
 *    - 缺点：短码可预测
 *    - 解决：可以加入混淆算法
 * 
 * 2. Hash算法（MurmurHash / MD5）
 *    - 优点：短码分散，不可预测
 *    - 缺点：可能冲突，需要处理
 *    - 处理：冲突时追加后缀重试
 * 
 * 3. 随机生成 + 去重
 *    - 优点：完全随机
 *    - 缺点：需要频繁检查是否存在
 *    - 优化：布隆过滤器快速判断
 * 
 * 4. 雪花算法
 *    - 优点：分布式唯一，有序
 *    - 缺点：ID太长，转换后短码长
 * 
 * 发号器设计（号段模式）：
 * - 数据库存储当前最大ID
 * - 每次取一个号段（如1000个ID）到内存
 * - 内存中自增分配，用完再取下一段
 * - 优点：减少数据库访问，高性能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortCodeGenerator {

    private final IdSegmentMapper segmentMapper;

    @Value("${short-url.code-length}")
    private int codeLength;

    @Value("${short-url.charset}")
    private String charset;

    private static final String BIZ_TAG = "short_url";

    /**
     * 当前号段的起始ID
     */
    private volatile long segmentStart = 0;

    /**
     * 当前号段的结束ID
     */
    private volatile long segmentEnd = 0;

    /**
     * 当前ID（原子操作）
     */
    private final AtomicLong currentId = new AtomicLong(0);

    /**
     * 初始化：加载第一个号段
     */
    @PostConstruct
    public void init() {
        loadNextSegment();
    }

    /**
     * 生成短码
     * 
     * 1. 获取自增ID
     * 2. 转换为62进制
     * 3. 补齐长度
     */
    public String generate() {
        long id = nextId();
        return encode(id);
    }

    /**
     * 获取下一个ID
     */
    private synchronized long nextId() {
        long id = currentId.incrementAndGet();
        
        // 当前号段用完，加载下一段
        if (id > segmentEnd) {
            loadNextSegment();
            id = currentId.incrementAndGet();
        }
        
        return id;
    }

    /**
     * 加载下一个号段
     */
    private void loadNextSegment() {
        int retryCount = 0;
        while (retryCount < 3) {
            IdSegment segment = segmentMapper.findByBizTag(BIZ_TAG);
            if (segment == null) {
                throw new RuntimeException("号段配置不存在: " + BIZ_TAG);
            }

            // 乐观锁更新
            int updated = segmentMapper.updateMaxId(BIZ_TAG, segment.getVersion());
            if (updated > 0) {
                // 更新成功，设置新号段
                segmentStart = segment.getMaxId() + 1;
                segmentEnd = segment.getMaxId() + segment.getStep();
                currentId.set(segmentStart - 1);
                
                log.info("加载新号段: start={}, end={}", segmentStart, segmentEnd);
                return;
            }
            
            retryCount++;
            log.warn("号段更新冲突，重试: {}", retryCount);
        }
        
        throw new RuntimeException("获取号段失败，重试次数超限");
    }

    /**
     * 将ID转换为62进制短码
     * 
     * 62进制字符集: 0-9 A-Z a-z
     */
    private String encode(long id) {
        StringBuilder sb = new StringBuilder();
        int base = charset.length(); // 62
        
        while (id > 0) {
            sb.append(charset.charAt((int) (id % base)));
            id /= base;
        }
        
        // 反转
        sb.reverse();
        
        // 补齐长度
        while (sb.length() < codeLength) {
            sb.insert(0, charset.charAt(0));
        }
        
        return sb.toString();
    }

    /**
     * 将短码解码为ID（可选，用于调试）
     */
    public long decode(String shortCode) {
        long id = 0;
        int base = charset.length();
        
        for (char c : shortCode.toCharArray()) {
            id = id * base + charset.indexOf(c);
        }
        
        return id;
    }
}

