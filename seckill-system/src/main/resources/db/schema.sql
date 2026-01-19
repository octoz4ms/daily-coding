-- 秒杀系统数据库初始化脚本

CREATE DATABASE IF NOT EXISTS seckill_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE seckill_db;

-- 商品表
CREATE TABLE IF NOT EXISTS t_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10, 2) NOT NULL COMMENT '商品原价',
    stock INT NOT NULL DEFAULT 0 COMMENT '总库存',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 秒杀活动表
CREATE TABLE IF NOT EXISTS t_seckill_activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '活动ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    seckill_price DECIMAL(10, 2) NOT NULL COMMENT '秒杀价格',
    total_stock INT NOT NULL COMMENT '秒杀库存总量',
    available_stock INT NOT NULL COMMENT '剩余库存',
    start_time DATETIME NOT NULL COMMENT '活动开始时间',
    end_time DATETIME NOT NULL COMMENT '活动结束时间',
    status TINYINT DEFAULT 0 COMMENT '活动状态: 0-未开始 1-进行中 2-已结束',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    INDEX idx_product_id (product_id),
    INDEX idx_start_time (start_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';

-- 秒杀订单表
CREATE TABLE IF NOT EXISTS t_seckill_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    activity_id BIGINT NOT NULL COMMENT '秒杀活动ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    seckill_price DECIMAL(10, 2) NOT NULL COMMENT '秒杀价格',
    status TINYINT DEFAULT 0 COMMENT '订单状态: 0-待支付 1-已支付 2-已取消 3-已退款',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    pay_time DATETIME COMMENT '支付时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX idx_user_activity (user_id, activity_id) COMMENT '确保一个用户只能参与一次秒杀',
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';

-- 插入测试数据
INSERT INTO t_product (name, description, price, stock) VALUES 
('iPhone 15 Pro Max', '苹果最新旗舰手机', 9999.00, 1000),
('MacBook Pro 14', 'M3 Pro芯片笔记本', 16999.00, 500),
('AirPods Pro 2', '主动降噪耳机', 1899.00, 2000);

INSERT INTO t_seckill_activity (product_id, seckill_price, total_stock, available_stock, start_time, end_time, status) VALUES 
(1, 6999.00, 100, 100, '2026-01-16 10:00:00', '2026-01-16 12:00:00', 1),
(2, 12999.00, 50, 50, '2026-01-16 14:00:00', '2026-01-16 16:00:00', 0),
(3, 999.00, 200, 200, '2026-01-16 20:00:00', '2026-01-16 22:00:00', 0);

