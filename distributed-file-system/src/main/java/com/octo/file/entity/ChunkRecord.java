package com.octo.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分片记录实体
 */
@Data
@TableName("t_chunk_record")
public class ChunkRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 上传任务ID
     */
    private String uploadId;

    /**
     * 分片序号(从1开始)
     */
    private Integer chunkNumber;

    /**
     * 分片大小
     */
    private Integer chunkSize;

    /**
     * 分片MD5
     */
    private String chunkMd5;

    /**
     * 临时存储路径
     */
    private String storagePath;

    /**
     * 状态: 0-上传中 1-上传完成
     */
    private Integer status;

    private LocalDateTime createTime;
}

