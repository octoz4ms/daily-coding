package com.octo.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 初始化上传请求
 */
@Data
public class InitUploadRequest {

    /**
     * 文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件大小(字节)
     */
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    /**
     * 文件MD5值
     */
    @NotBlank(message = "文件MD5不能为空")
    private String fileMd5;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 上传者ID
     */
    private Long uploaderId;
}

