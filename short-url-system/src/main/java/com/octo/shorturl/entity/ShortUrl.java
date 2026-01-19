package com.octo.shorturl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短链接实体
 */
@Data
@TableName("t_short_url")
public class ShortUrl {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 短码
     */
    private String shortCode;

    /**
     * 原始长链接
     */
    private String longUrl;

    /**
     * 长链接Hash值
     */
    private String longUrlHash;

    /**
     * 短链接域名
     */
    private String domain;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 状态: 0-禁用 1-正常
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 获取完整短链接
     */
    public String getFullShortUrl() {
        return domain + "/" + shortCode;
    }
}

