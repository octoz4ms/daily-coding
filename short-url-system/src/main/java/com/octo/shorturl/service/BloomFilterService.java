package com.octo.shorturl.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 布隆过滤器服务
 * 
 * 面试重点：布隆过滤器原理和应用
 * 
 * 原理：
 * 1. 初始化一个很长的二进制数组（bit array）
 * 2. 使用多个Hash函数计算元素的多个位置
 * 3. 将这些位置置为1
 * 4. 查询时检查所有位置是否都为1
 * 
 * 特点：
 * - 空间效率高：1亿数据只需约120MB
 * - 查询速度快：O(k)，k是Hash函数个数
 * - 存在误判：可能把不存在的判断为存在
 * - 不存在误判：存在的一定判断为存在
 * - 不支持删除：置1后无法置0
 * 
 * 应用场景：
 * 1. 短链接去重：快速判断短码是否已存在
 * 2. 缓存穿透防护：过滤不存在的Key
 * 3. 垃圾邮件过滤
 * 4. 爬虫URL去重
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BloomFilterService {

    private final RedissonClient redissonClient;

    @Value("${short-url.bloom-filter.expected-insertions}")
    private long expectedInsertions;

    @Value("${short-url.bloom-filter.false-probability}")
    private double falseProbability;

    @Value("${short-url.bloom-filter.key}")
    private String bloomFilterKey;

    private RBloomFilter<String> bloomFilter;

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter(bloomFilterKey);
        
        // 初始化布隆过滤器（如果未初始化）
        // expectedInsertions: 预期元素数量
        // falseProbability: 误判率
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(expectedInsertions, falseProbability);
            log.info("布隆过滤器初始化成功: key={}, expectedInsertions={}, falseProbability={}", 
                    bloomFilterKey, expectedInsertions, falseProbability);
        }
    }

    /**
     * 添加元素到布隆过滤器
     * 
     * @param element 元素
     * @return true-新增成功 false-可能已存在
     */
    public boolean add(String element) {
        return bloomFilter.add(element);
    }

    /**
     * 判断元素是否可能存在
     * 
     * 注意：返回true可能误判，返回false一定不存在
     * 
     * @param element 元素
     * @return true-可能存在 false-一定不存在
     */
    public boolean mightContain(String element) {
        return bloomFilter.contains(element);
    }

    /**
     * 获取布隆过滤器中的元素数量（估算）
     */
    public long count() {
        return bloomFilter.count();
    }

    /**
     * 获取预期元素数量
     */
    public long getExpectedInsertions() {
        return bloomFilter.getExpectedInsertions();
    }

    /**
     * 获取误判率
     */
    public double getFalseProbability() {
        return bloomFilter.getFalseProbability();
    }
}

