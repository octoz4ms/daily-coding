package com.octo.file.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MinIO服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 上传文件
     */
    public void uploadFile(String objectName, InputStream inputStream, 
                           long size, String contentType) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(inputStream, size, -1)
                .contentType(contentType)
                .build());
        
        log.info("文件上传成功: {}", objectName);
    }

    /**
     * 合并分片文件
     * 
     * 使用MinIO的composeObject API合并分片
     */
    public void mergeChunks(String objectName, List<String> chunkObjectNames) throws Exception {
        List<ComposeSource> sources = chunkObjectNames.stream()
                .map(chunkName -> ComposeSource.builder()
                        .bucket(bucketName)
                        .object(chunkName)
                        .build())
                .toList();

        minioClient.composeObject(ComposeObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .sources(sources)
                .build());
        
        log.info("分片合并成功: {}, 分片数: {}", objectName, chunkObjectNames.size());
    }

    /**
     * 上传分片
     */
    public void uploadChunk(String chunkObjectName, InputStream inputStream, 
                            long size) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(chunkObjectName)
                .stream(inputStream, size, -1)
                .build());
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }

    /**
     * 批量删除分片
     */
    public void deleteChunks(List<String> chunkObjectNames) {
        for (String chunkName : chunkObjectNames) {
            try {
                deleteFile(chunkName);
            } catch (Exception e) {
                log.warn("删除分片失败: {}", chunkName);
            }
        }
    }

    /**
     * 获取文件访问URL
     */
    public String getFileUrl(String objectName) {
        return endpoint + "/" + bucketName + "/" + objectName;
    }

    /**
     * 获取预签名URL（临时访问）
     */
    public String getPresignedUrl(String objectName, int expireMinutes) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .method(Method.GET)
                .expiry(expireMinutes, TimeUnit.MINUTES)
                .build());
    }

    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

