package com.octo.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息实体
 */
@Data
@TableName("t_file_info")
public class FileInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件MD5值 - 用于秒传检测
     */
    private String fileMd5;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 存储路径
     */
    private String storagePath;

    /**
     * MinIO桶名
     */
    private String bucketName;

    /**
     * MinIO对象名
     */
    private String objectName;

    /**
     * 访问URL
     */
    private String url;

    /**
     * 上传状态: 0-上传中 1-上传完成 2-上传失败
     */
    private Integer uploadStatus;

    /**
     * 上传者ID
     */
    private Long uploaderId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 上传状态枚举
     */
    public enum UploadStatus {
        UPLOADING(0, "上传中"),
        COMPLETED(1, "上传完成"),
        FAILED(2, "上传失败");

        private final int code;
        private final String desc;

        UploadStatus(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }
    }
}

