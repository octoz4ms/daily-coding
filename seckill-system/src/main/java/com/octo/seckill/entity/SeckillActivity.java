package com.octo.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动实体
 */
@Data
@TableName("t_seckill_activity")
public class SeckillActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;

    /**
     * 秒杀库存总量
     */
    private Integer totalStock;

    /**
     * 剩余库存
     */
    private Integer availableStock;

    /**
     * 活动开始时间
     */
    private LocalDateTime startTime;

    /**
     * 活动结束时间
     */
    private LocalDateTime endTime;

    /**
     * 活动状态: 0-未开始 1-进行中 2-已结束
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号 - 防止超卖的关键
     */
    @Version
    private Integer version;

    /**
     * 判断活动是否在进行中
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == 1 && now.isAfter(startTime) && now.isBefore(endTime);
    }
}

