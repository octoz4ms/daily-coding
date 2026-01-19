package com.octo.file.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 初始化上传响应
 */
@Data
@Builder
public class InitUploadResponse {

    /**
     * 是否秒传成功
     */
    private Boolean instantUpload;

    /**
     * 上传任务ID（非秒传时返回）
     */
    private String uploadId;

    /**
     * 文件URL（秒传时直接返回）
     */
    private String fileUrl;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 分片大小
     */
    private Integer chunkSize;

    /**
     * 已上传的分片序号列表（断点续传）
     */
    private List<Integer> uploadedChunks;

    /**
     * 秒传成功
     */
    public static InitUploadResponse instantSuccess(String fileUrl) {
        return InitUploadResponse.builder()
                .instantUpload(true)
                .fileUrl(fileUrl)
                .build();
    }

    /**
     * 需要分片上传
     */
    public static InitUploadResponse needUpload(String uploadId, Integer totalChunks, 
                                                 Integer chunkSize, List<Integer> uploadedChunks) {
        return InitUploadResponse.builder()
                .instantUpload(false)
                .uploadId(uploadId)
                .totalChunks(totalChunks)
                .chunkSize(chunkSize)
                .uploadedChunks(uploadedChunks)
                .build();
    }
}

