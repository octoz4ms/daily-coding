package com.octo.shorturl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 号段表实体（发号器）
 */
@Data
@TableName("t_id_segment")
public class IdSegment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务标识
     */
    private String bizTag;

    /**
     * 当前最大ID
     */
    private Long maxId;

    /**
     * 步长
     */
    private Integer step;

    /**
     * 描述
     */
    private String description;

    private LocalDateTime updateTime;

    /**
     * 乐观锁版本
     */
    @Version
    private Integer version;
}

