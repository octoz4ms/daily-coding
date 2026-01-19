package com.octo.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分片上传任务实体
 */
@Data
@TableName("t_chunk_upload_task")
public class ChunkUploadTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 上传任务ID
     */
    private String uploadId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件总大小
     */
    private Long fileSize;

    /**
     * 文件MD5
     */
    private String fileMd5;

    /**
     * 分片大小
     */
    private Integer chunkSize;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 已上传分片数
     */
    private Integer uploadedChunks;

    /**
     * 状态: 0-上传中 1-已完成 2-已取消 3-已过期
     */
    private Integer status;

    /**
     * MinIO桶名
     */
    private String bucketName;

    /**
     * MinIO对象名
     */
    private String objectName;

    /**
     * 上传者ID
     */
    private Long uploaderId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 任务状态枚举
     */
    public enum Status {
        UPLOADING(0, "上传中"),
        COMPLETED(1, "已完成"),
        CANCELLED(2, "已取消"),
        EXPIRED(3, "已过期");

        private final int code;
        private final String desc;

        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }
    }
}

