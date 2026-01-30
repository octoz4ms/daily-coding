-- =============================================
-- 订单支付系统数据库初始化脚本
-- 使用方法：先创建数据库，然后执行此脚本
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS order_payment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE order_payment;

-- =============================================
-- 表结构
-- =============================================

-- 商品表
DROP TABLE IF EXISTS t_product;
CREATE TABLE t_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description VARCHAR(500) COMMENT '商品描述',
    price DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
    image_url VARCHAR(500) COMMENT '商品图片',
    status TINYINT DEFAULT 1 COMMENT '商品状态：0-下架 1-上架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 库存表
DROP TABLE IF EXISTS t_stock;
CREATE TABLE t_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    total_stock INT NOT NULL DEFAULT 0 COMMENT '总库存',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    locked_stock INT NOT NULL DEFAULT 0 COMMENT '锁定库存',
    sold_stock INT NOT NULL DEFAULT 0 COMMENT '已售库存',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- 订单表
DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(50) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) COMMENT '商品名称',
    quantity INT NOT NULL DEFAULT 1 COMMENT '商品数量',
    unit_price DECIMAL(10, 2) NOT NULL COMMENT '商品单价',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '商品总价',
    discount_amount DECIMAL(10, 2) DEFAULT 0.00 COMMENT '优惠金额',
    freight_amount DECIMAL(10, 2) DEFAULT 0.00 COMMENT '运费',
    pay_amount DECIMAL(10, 2) NOT NULL COMMENT '实付金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0-待支付 1-已支付 2-已发货 3-已签收 4-已完成 5-已取消 6-已关闭 7-退款中 8-已退款',
    receiver_name VARCHAR(50) COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) COMMENT '收货人电话',
    receiver_address VARCHAR(200) COMMENT '收货地址',
    remark VARCHAR(500) COMMENT '订单备注',
    pay_time DATETIME COMMENT '支付时间',
    ship_time DATETIME COMMENT '发货时间',
    receive_time DATETIME COMMENT '签收时间',
    complete_time DATETIME COMMENT '完成时间',
    cancel_time DATETIME COMMENT '取消时间',
    cancel_reason VARCHAR(200) COMMENT '取消原因',
    expire_time DATETIME COMMENT '订单过期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 支付单表
DROP TABLE IF EXISTS t_payment_order;
CREATE TABLE t_payment_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    payment_no VARCHAR(50) NOT NULL COMMENT '支付单号',
    order_no VARCHAR(50) NOT NULL COMMENT '关联订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10, 2) NOT NULL COMMENT '支付金额',
    payment_method TINYINT NOT NULL COMMENT '支付方式：1-微信 2-支付宝 3-银行卡 4-余额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0-待支付 1-支付中 2-支付成功 3-支付失败 4-已关闭 5-已退款 6-部分退款',
    transaction_id VARCHAR(100) COMMENT '第三方支付交易号',
    description VARCHAR(200) COMMENT '支付描述',
    notify_url VARCHAR(500) COMMENT '回调通知地址',
    return_url VARCHAR(500) COMMENT '支付成功跳转地址',
    pay_url VARCHAR(500) COMMENT '支付二维码/链接',
    prepay_id VARCHAR(100) COMMENT '预支付ID',
    pay_time DATETIME COMMENT '支付时间',
    expire_time DATETIME COMMENT '过期时间',
    callback_data TEXT COMMENT '回调原始数据',
    callback_time DATETIME COMMENT '回调时间',
    error_msg VARCHAR(500) COMMENT '错误信息',
    attach VARCHAR(500) COMMENT '附加数据',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    UNIQUE KEY uk_payment_no (payment_no),
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单表';

-- =============================================
-- 初始化测试数据
-- =============================================

-- 插入测试商品
INSERT INTO t_product (id, name, description, price, status) VALUES 
(1, 'iPhone 15 Pro', 'Apple iPhone 15 Pro 256GB 原色钛金属', 8999.00, 1),
(2, 'MacBook Pro 14', 'Apple MacBook Pro 14英寸 M3 Pro芯片 18GB内存 512GB固态', 16999.00, 1),
(3, 'AirPods Pro 2', 'Apple AirPods Pro (第二代) 配MagSafe充电盒', 1899.00, 1),
(4, 'iPad Pro 12.9', 'Apple iPad Pro 12.9英寸 M2芯片 256GB WLAN版', 9299.00, 1),
(5, 'Apple Watch Ultra 2', 'Apple Watch Ultra 2 GPS+蜂窝网络 49毫米钛金属表壳', 6499.00, 1);

-- 插入测试库存
INSERT INTO t_stock (product_id, total_stock, available_stock, locked_stock, sold_stock) VALUES 
(1, 1000, 1000, 0, 0),
(2, 500, 500, 0, 0),
(3, 2000, 2000, 0, 0),
(4, 800, 800, 0, 0),
(5, 300, 300, 0, 0);

-- =============================================
-- 完成提示
-- =============================================
SELECT '数据库初始化完成！' AS message;
SELECT COUNT(*) AS '商品数量' FROM t_product;
SELECT COUNT(*) AS '库存记录数' FROM t_stock;

