package com.octo.shorturl.service;

import com.google.common.hash.Hashing;
import com.octo.shorturl.entity.ShortUrl;
import com.octo.shorturl.mapper.ShortUrlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 短链接服务
 * 
 * 核心流程：
 * 
 * 创建短链接：
 * 1. 计算长链接Hash，检查是否已存在（去重）
 * 2. 生成短码（发号器 + 62进制）
 * 3. 布隆过滤器添加短码
 * 4. 存储到数据库
 * 5. 缓存到Redis
 * 
 * 访问短链接：
 * 1. 布隆过滤器快速判断（防止缓存穿透）
 * 2. 查询Redis缓存
 * 3. 缓存未命中，查询数据库
 * 4. 回写缓存
 * 5. 302重定向到原始URL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private final ShortUrlMapper shortUrlMapper;
    private final ShortCodeGenerator codeGenerator;
    private final BloomFilterService bloomFilter;
    private final StringRedisTemplate redisTemplate;

    @Value("${short-url.domain}")
    private String domain;

    @Value("${short-url.cache.prefix}")
    private String cachePrefix;

    @Value("${short-url.cache.expire-days}")
    private int cacheExpireDays;

    /**
     * 创建短链接
     * 
     * @param longUrl 原始长链接
     * @param creatorId 创建者ID
     * @param expireTime 过期时间（可选）
     * @return 完整短链接
     */
    @Transactional(rollbackFor = Exception.class)
    public String createShortUrl(String longUrl, Long creatorId, LocalDateTime expireTime) {
        // 1. 计算长链接Hash，检查是否已存在
        String urlHash = hashUrl(longUrl);
        ShortUrl existing = shortUrlMapper.findByLongUrlHash(urlHash);
        if (existing != null) {
            log.info("长链接已存在短码: longUrl={}, shortCode={}", longUrl, existing.getShortCode());
            return existing.getFullShortUrl();
        }

        // 2. 生成短码
        String shortCode = codeGenerator.generate();

        // 3. 布隆过滤器添加
        bloomFilter.add(shortCode);

        // 4. 存储到数据库
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setShortCode(shortCode);
        shortUrl.setLongUrl(longUrl);
        shortUrl.setLongUrlHash(urlHash);
        shortUrl.setDomain(domain);
        shortUrl.setCreatorId(creatorId);
        shortUrl.setExpireTime(expireTime);
        shortUrl.setStatus(1);
        shortUrlMapper.insert(shortUrl);

        // 5. 缓存到Redis
        String cacheKey = getCacheKey(shortCode);
        redisTemplate.opsForValue().set(cacheKey, longUrl, cacheExpireDays, TimeUnit.DAYS);

        log.info("创建短链接成功: shortCode={}, longUrl={}", shortCode, longUrl);

        return shortUrl.getFullShortUrl();
    }

    /**
     * 获取原始链接
     * 
     * @param shortCode 短码
     * @return 原始长链接
     */
    public String getLongUrl(String shortCode) {
        // 1. 布隆过滤器快速判断（防止缓存穿透）
        if (!bloomFilter.mightContain(shortCode)) {
            log.debug("布隆过滤器判断短码不存在: {}", shortCode);
            return null;
        }

        // 2. 查询Redis缓存
        String cacheKey = getCacheKey(shortCode);
        String longUrl = redisTemplate.opsForValue().get(cacheKey);
        if (longUrl != null) {
            return longUrl;
        }

        // 3. 缓存未命中，查询数据库
        ShortUrl shortUrl = shortUrlMapper.findByShortCode(shortCode);
        if (shortUrl == null) {
            // 布隆过滤器误判，设置空值缓存（防止穿透）
            redisTemplate.opsForValue().set(cacheKey, "", 5, TimeUnit.MINUTES);
            return null;
        }

        // 4. 检查是否过期
        if (shortUrl.getExpireTime() != null && 
                shortUrl.getExpireTime().isBefore(LocalDateTime.now())) {
            log.info("短链接已过期: {}", shortCode);
            return null;
        }

        // 5. 回写缓存
        redisTemplate.opsForValue().set(cacheKey, shortUrl.getLongUrl(), 
                cacheExpireDays, TimeUnit.DAYS);

        return shortUrl.getLongUrl();
    }

    /**
     * 获取短链接信息
     */
    public ShortUrl getShortUrlInfo(String shortCode) {
        return shortUrlMapper.findByShortCode(shortCode);
    }

    /**
     * 禁用短链接
     */
    public void disableShortUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlMapper.findByShortCode(shortCode);
        if (shortUrl != null) {
            shortUrl.setStatus(0);
            shortUrlMapper.updateById(shortUrl);
            // 删除缓存
            redisTemplate.delete(getCacheKey(shortCode));
            log.info("短链接已禁用: {}", shortCode);
        }
    }

    /**
     * 计算URL的Hash值
     */
    private String hashUrl(String url) {
        return Hashing.murmur3_128()
                .hashString(url, StandardCharsets.UTF_8)
                .toString();
    }

    /**
     * 获取缓存Key
     */
    private String getCacheKey(String shortCode) {
        return cachePrefix + shortCode;
    }
}

