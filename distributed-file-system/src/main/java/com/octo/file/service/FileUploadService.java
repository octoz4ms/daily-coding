package com.octo.file.service;

import com.octo.file.dto.InitUploadRequest;
import com.octo.file.dto.InitUploadResponse;
import com.octo.file.entity.ChunkRecord;
import com.octo.file.entity.ChunkUploadTask;
import com.octo.file.entity.FileInfo;
import com.octo.file.mapper.ChunkRecordMapper;
import com.octo.file.mapper.ChunkUploadTaskMapper;
import com.octo.file.mapper.FileInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务
 * 
 * 面试重点：大文件如何上传？
 * 
 * 核心方案：分片上传 + 断点续传 + 秒传
 * 
 * 1. 分片上传
 *    - 前端将大文件切分为固定大小的分片（如5MB）
 *    - 并行上传多个分片，提高上传速度
 *    - 所有分片上传完成后，服务端合并
 * 
 * 2. 断点续传
 *    - 服务端记录已上传的分片
 *    - 上传中断后，只需上传未完成的分片
 *    - 通过uploadId标识上传任务
 * 
 * 3. 秒传
 *    - 上传前计算文件MD5
 *    - 服务端检查是否已存在相同MD5的文件
 *    - 存在则直接返回，无需实际上传
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileInfoMapper fileInfoMapper;
    private final ChunkUploadTaskMapper taskMapper;
    private final ChunkRecordMapper chunkRecordMapper;
    private final MinioService minioService;

    @Value("${file.upload.chunk-size}")
    private int chunkSize;

    @Value("${file.upload.chunk-expire-hours}")
    private int chunkExpireHours;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * 初始化上传
     * 
     * 流程：
     * 1. 秒传检测 - 检查MD5是否已存在
     * 2. 断点续传检测 - 检查是否有未完成的上传任务
     * 3. 创建新上传任务
     */
    public InitUploadResponse initUpload(InitUploadRequest request) {
        String fileMd5 = request.getFileMd5();
        Long uploaderId = request.getUploaderId();

        // 1. 秒传检测
        FileInfo existingFile = fileInfoMapper.findByMd5(fileMd5);
        if (existingFile != null) {
            log.info("秒传成功: fileName={}, md5={}", request.getFileName(), fileMd5);
            return InitUploadResponse.instantSuccess(existingFile.getUrl());
        }

        // 2. 断点续传检测
        ChunkUploadTask existingTask = taskMapper.findUnfinishedByMd5(fileMd5, uploaderId);
        if (existingTask != null) {
            List<Integer> uploadedChunks = chunkRecordMapper.findUploadedChunkNumbers(existingTask.getUploadId());
            log.info("断点续传: uploadId={}, uploaded={}/{}", 
                    existingTask.getUploadId(), uploadedChunks.size(), existingTask.getTotalChunks());
            return InitUploadResponse.needUpload(
                    existingTask.getUploadId(),
                    existingTask.getTotalChunks(),
                    existingTask.getChunkSize(),
                    uploadedChunks
            );
        }

        // 3. 创建新上传任务
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        int totalChunks = (int) Math.ceil((double) request.getFileSize() / chunkSize);
        String objectName = generateObjectName(request.getFileName());

        ChunkUploadTask task = new ChunkUploadTask();
        task.setUploadId(uploadId);
        task.setFileName(request.getFileName());
        task.setFileSize(request.getFileSize());
        task.setFileMd5(fileMd5);
        task.setChunkSize(chunkSize);
        task.setTotalChunks(totalChunks);
        task.setUploadedChunks(0);
        task.setStatus(ChunkUploadTask.Status.UPLOADING.getCode());
        task.setBucketName(bucketName);
        task.setObjectName(objectName);
        task.setUploaderId(uploaderId);
        task.setExpireTime(LocalDateTime.now().plusHours(chunkExpireHours));

        taskMapper.insert(task);

        log.info("创建上传任务: uploadId={}, fileName={}, totalChunks={}", 
                uploadId, request.getFileName(), totalChunks);

        return InitUploadResponse.needUpload(uploadId, totalChunks, chunkSize, new ArrayList<>());
    }

    /**
     * 上传分片
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean uploadChunk(String uploadId, Integer chunkNumber, MultipartFile file) {
        // 1. 检查任务是否存在
        ChunkUploadTask task = taskMapper.findByUploadId(uploadId);
        if (task == null || task.getStatus() != ChunkUploadTask.Status.UPLOADING.getCode()) {
            log.warn("上传任务不存在或已结束: uploadId={}", uploadId);
            return false;
        }

        // 2. 检查分片是否已上传（幂等）
        if (chunkRecordMapper.checkChunkUploaded(uploadId, chunkNumber) > 0) {
            log.info("分片已上传，跳过: uploadId={}, chunkNumber={}", uploadId, chunkNumber);
            return true;
        }

        try {
            // 3. 上传分片到MinIO
            String chunkObjectName = getChunkObjectName(task.getObjectName(), chunkNumber);
            minioService.uploadChunk(chunkObjectName, file.getInputStream(), file.getSize());

            // 4. 记录分片信息
            ChunkRecord record = new ChunkRecord();
            record.setUploadId(uploadId);
            record.setChunkNumber(chunkNumber);
            record.setChunkSize((int) file.getSize());
            record.setChunkMd5(DigestUtils.md5Hex(file.getInputStream()));
            record.setStoragePath(chunkObjectName);
            record.setStatus(1);
            chunkRecordMapper.insert(record);

            // 5. 更新已上传分片数
            taskMapper.incrementUploadedChunks(uploadId);

            log.info("分片上传成功: uploadId={}, chunkNumber={}/{}", 
                    uploadId, chunkNumber, task.getTotalChunks());

            return true;
        } catch (Exception e) {
            log.error("分片上传失败: uploadId={}, chunkNumber={}", uploadId, chunkNumber, e);
            return false;
        }
    }

    /**
     * 合并分片，完成上传
     */
    @Transactional(rollbackFor = Exception.class)
    public String mergeChunks(String uploadId) {
        ChunkUploadTask task = taskMapper.findByUploadId(uploadId);
        if (task == null) {
            throw new RuntimeException("上传任务不存在");
        }

        // 1. 检查所有分片是否已上传
        List<ChunkRecord> records = chunkRecordMapper.findByUploadId(uploadId);
        if (records.size() != task.getTotalChunks()) {
            throw new RuntimeException("分片未全部上传完成");
        }

        try {
            // 2. 合并分片
            List<String> chunkObjectNames = records.stream()
                    .map(ChunkRecord::getStoragePath)
                    .toList();
            minioService.mergeChunks(task.getObjectName(), chunkObjectNames);

            // 3. 保存文件信息
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(task.getFileName());
            fileInfo.setFileSize(task.getFileSize());
            fileInfo.setFileMd5(task.getFileMd5());
            fileInfo.setFileType(getFileExtension(task.getFileName()));
            fileInfo.setStoragePath(task.getObjectName());
            fileInfo.setBucketName(task.getBucketName());
            fileInfo.setObjectName(task.getObjectName());
            fileInfo.setUrl(minioService.getFileUrl(task.getObjectName()));
            fileInfo.setUploadStatus(FileInfo.UploadStatus.COMPLETED.getCode());
            fileInfo.setUploaderId(task.getUploaderId());
            fileInfoMapper.insert(fileInfo);

            // 4. 更新任务状态
            taskMapper.updateStatus(uploadId, ChunkUploadTask.Status.COMPLETED.getCode());

            // 5. 清理分片文件
            minioService.deleteChunks(chunkObjectNames);
            chunkRecordMapper.deleteByUploadId(uploadId);

            log.info("文件合并成功: uploadId={}, fileName={}", uploadId, task.getFileName());

            return fileInfo.getUrl();
        } catch (Exception e) {
            log.error("文件合并失败: uploadId={}", uploadId, e);
            throw new RuntimeException("文件合并失败", e);
        }
    }

    /**
     * 获取上传进度
     */
    public ChunkUploadTask getUploadProgress(String uploadId) {
        return taskMapper.findByUploadId(uploadId);
    }

    /**
     * 取消上传
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelUpload(String uploadId) {
        ChunkUploadTask task = taskMapper.findByUploadId(uploadId);
        if (task == null) {
            return;
        }

        // 删除已上传的分片
        List<ChunkRecord> records = chunkRecordMapper.findByUploadId(uploadId);
        List<String> chunkNames = records.stream()
                .map(ChunkRecord::getStoragePath)
                .toList();
        minioService.deleteChunks(chunkNames);

        // 删除记录
        chunkRecordMapper.deleteByUploadId(uploadId);
        taskMapper.updateStatus(uploadId, ChunkUploadTask.Status.CANCELLED.getCode());

        log.info("上传已取消: uploadId={}", uploadId);
    }

    /**
     * 生成对象名称
     */
    private String generateObjectName(String fileName) {
        String ext = getFileExtension(fileName);
        String dateDir = LocalDateTime.now().toString().substring(0, 10).replace("-", "/");
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return dateDir + "/" + uuid + "." + ext;
    }

    /**
     * 获取分片对象名称
     */
    private String getChunkObjectName(String objectName, int chunkNumber) {
        return "chunks/" + objectName + ".chunk" + chunkNumber;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }
}

