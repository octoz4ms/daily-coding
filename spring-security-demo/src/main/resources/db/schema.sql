-- ========================================
-- Spring Security Demo 数据库建表脚本
-- 数据库：MySQL 8.0+
-- 字符集：utf8mb4
-- ========================================

-- 创建数据库（如果不存在）
-- CREATE DATABASE IF NOT EXISTS db_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE db_test;

-- ========================================
-- 1. 用户表
-- ========================================
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `user_no` VARCHAR(50) NOT NULL COMMENT '用户编号',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_no` (`user_no`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ========================================
-- 2. 角色表
-- ========================================
DROP TABLE IF EXISTS `tb_role`;
CREATE TABLE `tb_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_no` VARCHAR(50) NOT NULL COMMENT '角色编号',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码（如：ADMIN, USER）',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_no` (`role_no`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ========================================
-- 3. 权限表
-- ========================================
DROP TABLE IF EXISTS `tb_permission`;
CREATE TABLE `tb_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `perm_no` VARCHAR(50) NOT NULL COMMENT '权限编号',
    `perm_code` VARCHAR(100) NOT NULL COMMENT '权限编码（如：user:add, user:delete）',
    `perm_name` VARCHAR(50) NOT NULL COMMENT '权限名称',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '权限描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_perm_no` (`perm_no`),
    UNIQUE KEY `uk_perm_code` (`perm_code`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ========================================
-- 4. 用户-角色关联表
-- ========================================
DROP TABLE IF EXISTS `tb_user_role`;
CREATE TABLE `tb_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- ========================================
-- 5. 角色-权限关联表
-- ========================================
DROP TABLE IF EXISTS `tb_role_permission`;
CREATE TABLE `tb_role_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `perm_id` BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `perm_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_perm_id` (`perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';

-- ========================================
-- 初始化数据
-- ========================================

-- 初始化管理员用户（密码：123456，使用BCrypt加密）
INSERT INTO `tb_user` (`user_no`, `username`, `password`) VALUES
('U000000000001', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH'),
('U000000000002', 'user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH');

-- 初始化角色
INSERT INTO `tb_role` (`role_no`, `role_code`, `role_name`, `description`) VALUES
('R000000000001', 'ADMIN', '管理员', '系统管理员，拥有所有权限'),
('R000000000002', 'USER', '普通用户', '普通用户，拥有基本权限');

-- 初始化权限
INSERT INTO `tb_permission` (`perm_no`, `perm_code`, `perm_name`, `description`) VALUES
-- 用户管理权限
('P000000000001', 'user:list', '用户列表', '查看用户列表'),
('P000000000002', 'user:query', '用户详情', '查看用户详情'),
('P000000000003', 'user:add', '添加用户', '添加新用户'),
('P000000000004', 'user:edit', '编辑用户', '编辑用户信息'),
('P000000000005', 'user:delete', '删除用户', '删除用户'),
('P000000000006', 'user:assignRole', '分配角色', '为用户分配角色'),
-- 角色管理权限
('P000000000007', 'role:list', '角色列表', '查看角色列表'),
('P000000000008', 'role:query', '角色详情', '查看角色详情'),
('P000000000009', 'role:add', '添加角色', '添加新角色'),
('P000000000010', 'role:edit', '编辑角色', '编辑角色信息'),
('P000000000011', 'role:delete', '删除角色', '删除角色'),
('P000000000012', 'role:assignPermission', '分配权限', '为角色分配权限'),
-- 权限管理权限
('P000000000013', 'permission:list', '权限列表', '查看权限列表'),
('P000000000014', 'permission:query', '权限详情', '查看权限详情'),
('P000000000015', 'permission:add', '添加权限', '添加新权限'),
('P000000000016', 'permission:edit', '编辑权限', '编辑权限信息'),
('P000000000017', 'permission:delete', '删除权限', '删除权限');

-- 用户-角色关联（admin用户拥有管理员角色，user用户拥有普通用户角色）
INSERT INTO `tb_user_role` (`user_id`, `role_id`) VALUES
(1, 1),  -- admin -> ADMIN
(2, 2);  -- user -> USER

-- 角色-权限关联（管理员拥有所有权限）
INSERT INTO `tb_role_permission` (`role_id`, `perm_id`) VALUES
-- ADMIN角色拥有所有权限
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),
(1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12),
(1, 13), (1, 14), (1, 15), (1, 16), (1, 17),
-- USER角色拥有查看权限
(2, 1), (2, 2), (2, 7), (2, 8), (2, 13), (2, 14);


