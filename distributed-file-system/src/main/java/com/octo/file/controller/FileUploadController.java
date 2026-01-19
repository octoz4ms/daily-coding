package com.octo.file.controller;

import com.octo.file.common.Result;
import com.octo.file.dto.InitUploadRequest;
import com.octo.file.dto.InitUploadResponse;
import com.octo.file.entity.ChunkUploadTask;
import com.octo.file.service.FileUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 * 
 * API接口：
 * - POST /api/file/init     初始化上传（秒传检测）
 * - POST /api/file/chunk    上传分片
 * - POST /api/file/merge    合并分片
 * - GET  /api/file/progress 查询进度
 * - POST /api/file/cancel   取消上传
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService uploadService;

    /**
     * 初始化上传
     * 
     * 1. 检测秒传 - 如果文件已存在，直接返回URL
     * 2. 检测断点续传 - 如果有未完成任务，返回已上传分片
     * 3. 创建新任务 - 返回uploadId和分片信息
     */
    @PostMapping("/init")
    public Result<InitUploadResponse> initUpload(@Valid @RequestBody InitUploadRequest request) {
        InitUploadResponse response = uploadService.initUpload(request);
        return Result.success(response);
    }

    /**
     * 上传分片
     * 
     * @param uploadId 上传任务ID
     * @param chunkNumber 分片序号（从1开始）
     * @param file 分片文件
     */
    @PostMapping("/chunk")
    public Result<Boolean> uploadChunk(
            @RequestParam String uploadId,
            @RequestParam Integer chunkNumber,
            @RequestParam("file") MultipartFile file) {
        boolean success = uploadService.uploadChunk(uploadId, chunkNumber, file);
        return success ? Result.success(true) : Result.fail("分片上传失败");
    }

    /**
     * 合并分片
     * 
     * @param uploadId 上传任务ID
     */
    @PostMapping("/merge")
    public Result<String> mergeChunks(@RequestParam String uploadId) {
        String fileUrl = uploadService.mergeChunks(uploadId);
        return Result.success("上传成功", fileUrl);
    }

    /**
     * 查询上传进度
     */
    @GetMapping("/progress")
    public Result<ChunkUploadTask> getProgress(@RequestParam String uploadId) {
        ChunkUploadTask task = uploadService.getUploadProgress(uploadId);
        return task != null ? Result.success(task) : Result.fail("任务不存在");
    }

    /**
     * 取消上传
     */
    @PostMapping("/cancel")
    public Result<Void> cancelUpload(@RequestParam String uploadId) {
        uploadService.cancelUpload(uploadId);
        return Result.success();
    }
}

