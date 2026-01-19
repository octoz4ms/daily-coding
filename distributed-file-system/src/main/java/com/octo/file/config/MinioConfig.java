package com.octo.file.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置
 * 
 * MinIO是高性能的分布式对象存储服务，兼容Amazon S3 API
 * 
 * 面试要点 - 为什么选择MinIO？
 * 
 * 1. 高性能
 *    - 支持大规模并发读写
 *    - 存储效率高
 * 
 * 2. 分布式
 *    - 支持纠删码，数据冗余
 *    - 横向扩展
 * 
 * 3. S3兼容
 *    - 兼容AWS S3 API
 *    - 迁移方便
 * 
 * 4. 开源免费
 *    - Apache 2.0协议
 */
@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 初始化存储桶
     */
    @PostConstruct
    public void initBucket() {
        try {
            MinioClient client = minioClient();
            boolean exists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("MinIO存储桶创建成功: {}", bucketName);
            }
        } catch (Exception e) {
            log.warn("MinIO存储桶初始化失败（MinIO可能未启动）: {}", e.getMessage());
        }
    }
}

