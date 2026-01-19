package com.octo.seckill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 秒杀请求DTO
 */
@Data
public class SeckillRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 秒杀活动ID
     */
    @NotNull(message = "活动ID不能为空")
    private Long activityId;
}

