package com.octo.shorturl.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

/**
 * 创建短链接请求
 */
@Data
public class CreateShortUrlRequest {

    /**
     * 原始长链接
     */
    @NotBlank(message = "长链接不能为空")
    @URL(message = "请输入有效的URL")
    private String longUrl;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
}

