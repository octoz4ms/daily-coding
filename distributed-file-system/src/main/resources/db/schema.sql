-- 分布式文件系统数据库初始化脚本

CREATE DATABASE IF NOT EXISTS file_system_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE file_system_db;

-- 文件信息表
CREATE TABLE IF NOT EXISTS t_file_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    file_md5 VARCHAR(32) NOT NULL COMMENT '文件MD5值',
    file_type VARCHAR(50) COMMENT '文件类型',
    storage_path VARCHAR(500) NOT NULL COMMENT '存储路径',
    bucket_name VARCHAR(100) COMMENT 'MinIO桶名',
    object_name VARCHAR(255) COMMENT 'MinIO对象名',
    url VARCHAR(500) COMMENT '访问URL',
    upload_status TINYINT DEFAULT 0 COMMENT '上传状态: 0-上传中 1-上传完成 2-上传失败',
    uploader_id BIGINT COMMENT '上传者ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_file_md5 (file_md5) COMMENT '用于秒传检测',
    INDEX idx_uploader_id (uploader_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- 分片上传任务表
CREATE TABLE IF NOT EXISTS t_chunk_upload_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    upload_id VARCHAR(64) NOT NULL UNIQUE COMMENT '上传任务ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_size BIGINT NOT NULL COMMENT '文件总大小',
    file_md5 VARCHAR(32) NOT NULL COMMENT '文件MD5',
    chunk_size INT NOT NULL COMMENT '分片大小',
    total_chunks INT NOT NULL COMMENT '总分片数',
    uploaded_chunks INT DEFAULT 0 COMMENT '已上传分片数',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-上传中 1-已完成 2-已取消 3-已过期',
    bucket_name VARCHAR(100) COMMENT 'MinIO桶名',
    object_name VARCHAR(255) COMMENT 'MinIO对象名',
    uploader_id BIGINT COMMENT '上传者ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expire_time DATETIME COMMENT '过期时间',
    INDEX idx_file_md5 (file_md5),
    INDEX idx_uploader_id (uploader_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分片上传任务表';

-- 分片记录表
CREATE TABLE IF NOT EXISTS t_chunk_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    upload_id VARCHAR(64) NOT NULL COMMENT '上传任务ID',
    chunk_number INT NOT NULL COMMENT '分片序号(从1开始)',
    chunk_size INT NOT NULL COMMENT '分片大小',
    chunk_md5 VARCHAR(32) COMMENT '分片MD5',
    storage_path VARCHAR(500) COMMENT '临时存储路径',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-上传中 1-上传完成',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_upload_chunk (upload_id, chunk_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分片记录表';

