-- 短链接系统数据库初始化脚本

CREATE DATABASE IF NOT EXISTS short_url_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE short_url_db;

-- 短链接表
CREATE TABLE IF NOT EXISTS t_short_url (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    short_code VARCHAR(10) NOT NULL UNIQUE COMMENT '短码',
    long_url VARCHAR(2048) NOT NULL COMMENT '原始长链接',
    long_url_hash VARCHAR(64) NOT NULL COMMENT '长链接Hash值',
    domain VARCHAR(100) COMMENT '短链接域名',
    creator_id BIGINT COMMENT '创建者ID',
    expire_time DATETIME COMMENT '过期时间',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_long_url_hash (long_url_hash) COMMENT '用于查找已存在的短链接',
    INDEX idx_creator_id (creator_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链接表';

-- 访问统计表
CREATE TABLE IF NOT EXISTS t_access_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(10) NOT NULL COMMENT '短码',
    ip VARCHAR(50) COMMENT '访问IP',
    user_agent VARCHAR(500) COMMENT 'User-Agent',
    referer VARCHAR(500) COMMENT '来源页面',
    device_type VARCHAR(20) COMMENT '设备类型: PC/Mobile/Tablet',
    browser VARCHAR(50) COMMENT '浏览器',
    os VARCHAR(50) COMMENT '操作系统',
    country VARCHAR(50) COMMENT '国家',
    province VARCHAR(50) COMMENT '省份',
    city VARCHAR(50) COMMENT '城市',
    access_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    INDEX idx_short_code (short_code),
    INDEX idx_access_time (access_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问日志表';

-- 访问统计汇总表（按天）
CREATE TABLE IF NOT EXISTS t_access_stats_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(10) NOT NULL COMMENT '短码',
    stat_date DATE NOT NULL COMMENT '统计日期',
    pv INT DEFAULT 0 COMMENT '页面访问量',
    uv INT DEFAULT 0 COMMENT '独立访客数',
    ip_count INT DEFAULT 0 COMMENT 'IP数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_code_date (short_code, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日访问统计表';

-- 号段表（发号器方案）
CREATE TABLE IF NOT EXISTS t_id_segment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_tag VARCHAR(50) NOT NULL UNIQUE COMMENT '业务标识',
    max_id BIGINT NOT NULL DEFAULT 0 COMMENT '当前最大ID',
    step INT NOT NULL DEFAULT 1000 COMMENT '步长',
    description VARCHAR(200) COMMENT '描述',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT DEFAULT 0 COMMENT '乐观锁版本'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='号段表';

-- 初始化号段
INSERT INTO t_id_segment (biz_tag, max_id, step, description) VALUES 
('short_url', 100000, 1000, '短链接ID发号器');

