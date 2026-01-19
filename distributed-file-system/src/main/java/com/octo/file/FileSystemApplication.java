package com.octo.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 分布式文件系统启动类
 * 
 * 核心技术点：
 * 1. MinIO - 分布式对象存储
 * 2. 分片上传 - 大文件切分为小块上传
 * 3. 断点续传 - 记录已上传分片，支持续传
 * 4. 文件秒传 - MD5去重，相同文件直接返回
 * 5. CDN加速 - 配合CDN分发文件
 * 
 * @author octo
 */
@SpringBootApplication
@MapperScan("com.octo.file.mapper")
@EnableScheduling
public class FileSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileSystemApplication.class, args);
    }
}

