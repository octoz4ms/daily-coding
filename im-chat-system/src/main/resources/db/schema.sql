-- IM即时通讯系统数据库初始化脚本

CREATE DATABASE IF NOT EXISTS im_chat_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE im_chat_db;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-离线 1-在线 2-忙碌',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 好友关系表
CREATE TABLE IF NOT EXISTS t_friendship (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    friend_id BIGINT NOT NULL COMMENT '好友ID',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-已删除 1-正常 2-黑名单',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_user_friend (user_id, friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

-- 会话表（私聊/群聊）
CREATE TABLE IF NOT EXISTS t_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    type TINYINT NOT NULL COMMENT '类型: 1-私聊 2-群聊',
    name VARCHAR(100) COMMENT '会话名称（群聊时为群名）',
    avatar VARCHAR(255) COMMENT '头像',
    owner_id BIGINT COMMENT '群主ID（群聊时）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 会话成员表
CREATE TABLE IF NOT EXISTS t_conversation_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    nickname VARCHAR(100) COMMENT '群内昵称',
    role TINYINT DEFAULT 0 COMMENT '角色: 0-成员 1-管理员 2-群主',
    muted TINYINT DEFAULT 0 COMMENT '是否禁言',
    last_read_msg_id BIGINT DEFAULT 0 COMMENT '最后已读消息ID',
    last_read_time DATETIME COMMENT '最后已读时间',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_conv_user (conversation_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话成员表';

-- 消息表（分表策略：按月分表）
CREATE TABLE IF NOT EXISTS t_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    msg_id VARCHAR(64) NOT NULL UNIQUE COMMENT '消息唯一ID（UUID）',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    msg_type TINYINT NOT NULL COMMENT '消息类型: 1-文本 2-图片 3-语音 4-视频 5-文件 6-位置 7-撤回',
    content TEXT COMMENT '消息内容',
    extra JSON COMMENT '扩展信息（图片尺寸、文件大小等）',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-发送中 1-已发送 2-已送达 3-已读 4-撤回',
    send_time DATETIME NOT NULL COMMENT '发送时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 离线消息表（用户离线时暂存）
CREATE TABLE IF NOT EXISTS t_offline_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '接收者ID',
    msg_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='离线消息表';

-- 消息已读回执表
CREATE TABLE IF NOT EXISTS t_message_receipt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    msg_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '已读用户ID',
    read_time DATETIME NOT NULL COMMENT '已读时间',
    UNIQUE INDEX idx_msg_user (msg_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息已读回执表';

-- 插入测试用户
INSERT INTO t_user (username, nickname, avatar, status) VALUES 
('alice', 'Alice', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Alice', 0),
('bob', 'Bob', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Bob', 0),
('charlie', 'Charlie', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Charlie', 0);

-- 插入好友关系
INSERT INTO t_friendship (user_id, friend_id) VALUES (1, 2), (2, 1), (1, 3), (3, 1), (2, 3), (3, 2);

-- 创建测试群聊
INSERT INTO t_conversation (type, name, avatar, owner_id) VALUES 
(2, '技术交流群', 'https://api.dicebear.com/7.x/shapes/svg?seed=group1', 1);

-- 添加群成员
INSERT INTO t_conversation_member (conversation_id, user_id, role) VALUES 
(1, 1, 2), (1, 2, 0), (1, 3, 0);

