-- 企业级用户管理系统数据库脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS eum_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eum_db;

-- 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              BIGINT          NOT NULL            COMMENT '用户ID',
    username        VARCHAR(32)     NOT NULL            COMMENT '用户名',
    password        VARCHAR(128)    NOT NULL            COMMENT '密码',
    nickname        VARCHAR(32)     DEFAULT ''          COMMENT '昵称',
    email           VARCHAR(64)     DEFAULT ''          COMMENT '邮箱',
    phone           VARCHAR(16)     DEFAULT ''          COMMENT '手机号',
    avatar          VARCHAR(256)    DEFAULT ''          COMMENT '头像URL',
    gender          TINYINT         DEFAULT 0           COMMENT '性别 0-未知 1-男 2-女',
    status          TINYINT         DEFAULT 1           COMMENT '状态 0-禁用 1-正常',
    last_login_time DATETIME        DEFAULT NULL        COMMENT '最后登录时间',
    last_login_ip   VARCHAR(64)     DEFAULT ''          COMMENT '最后登录IP',
    login_fail_count INT            DEFAULT 0           COMMENT '登录失败次数',
    lock_time       DATETIME        DEFAULT NULL        COMMENT '账号锁定时间',
    remark          VARCHAR(255)    DEFAULT ''          COMMENT '备注',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL        COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL        COMMENT '更新人ID',
    deleted         TINYINT         DEFAULT 0           COMMENT '删除标记 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_phone (phone),
    KEY idx_email (email),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id              BIGINT          NOT NULL            COMMENT '角色ID',
    code            VARCHAR(32)     NOT NULL            COMMENT '角色编码',
    name            VARCHAR(32)     NOT NULL            COMMENT '角色名称',
    description     VARCHAR(255)    DEFAULT ''          COMMENT '角色描述',
    sort            INT             DEFAULT 0           COMMENT '排序',
    status          TINYINT         DEFAULT 1           COMMENT '状态 0-禁用 1-正常',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL        COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL        COMMENT '更新人ID',
    deleted         TINYINT         DEFAULT 0           COMMENT '删除标记 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_status (status),
    KEY idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 权限表
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id              BIGINT          NOT NULL            COMMENT '权限ID',
    parent_id       BIGINT          DEFAULT 0           COMMENT '父级ID',
    code            VARCHAR(64)     NOT NULL            COMMENT '权限编码',
    name            VARCHAR(32)     NOT NULL            COMMENT '权限名称',
    type            TINYINT         NOT NULL            COMMENT '类型 1-目录 2-菜单 3-按钮',
    path            VARCHAR(128)    DEFAULT ''          COMMENT '路由路径',
    component       VARCHAR(128)    DEFAULT ''          COMMENT '组件路径',
    icon            VARCHAR(64)     DEFAULT ''          COMMENT '图标',
    sort            INT             DEFAULT 0           COMMENT '排序',
    visible         TINYINT         DEFAULT 1           COMMENT '是否可见 0-隐藏 1-显示',
    status          TINYINT         DEFAULT 1           COMMENT '状态 0-禁用 1-正常',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL        COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL        COMMENT '更新人ID',
    deleted         TINYINT         DEFAULT 0           COMMENT '删除标记 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_parent_id (parent_id),
    KEY idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 用户角色关联表
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id              BIGINT          NOT NULL            COMMENT '主键ID',
    user_id         BIGINT          NOT NULL            COMMENT '用户ID',
    role_id         BIGINT          NOT NULL            COMMENT '角色ID',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 角色权限关联表
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    id              BIGINT          NOT NULL            COMMENT '主键ID',
    role_id         BIGINT          NOT NULL            COMMENT '角色ID',
    permission_id   BIGINT          NOT NULL            COMMENT '权限ID',
    PRIMARY KEY (id),
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 登录日志表
DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log (
    id              BIGINT          NOT NULL            COMMENT '日志ID',
    user_id         BIGINT          DEFAULT NULL        COMMENT '用户ID',
    username        VARCHAR(32)     DEFAULT ''          COMMENT '用户名',
    type            TINYINT         DEFAULT 1           COMMENT '类型 1-登录 2-登出',
    ip              VARCHAR(64)     DEFAULT ''          COMMENT '登录IP',
    location        VARCHAR(64)     DEFAULT ''          COMMENT '登录地点',
    browser         VARCHAR(64)     DEFAULT ''          COMMENT '浏览器',
    os              VARCHAR(64)     DEFAULT ''          COMMENT '操作系统',
    status          TINYINT         DEFAULT 0           COMMENT '状态 0-失败 1-成功',
    message         VARCHAR(255)    DEFAULT ''          COMMENT '消息',
    login_time      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_username (username),
    KEY idx_login_time (login_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- ==================== 初始化数据 ====================

-- 初始化超级管理员用户 (密码: admin123, BCrypt加密)
INSERT INTO sys_user (id, username, password, nickname, email, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员', 'admin@example.com', 1);

-- 初始化普通测试用户 (密码: user123, BCrypt加密)
INSERT INTO sys_user (id, username, password, nickname, email, status) VALUES
(2, 'user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '普通用户', 'user@example.com', 1);

-- 初始化角色
INSERT INTO sys_role (id, code, name, description, sort, status) VALUES
(1, 'ADMIN', '超级管理员', '拥有系统所有权限', 1, 1),
(2, 'USER', '普通用户', '普通用户角色', 2, 1),
(3, 'GUEST', '访客', '只能查看，不能操作', 3, 1);

-- 初始化权限
-- 系统管理
INSERT INTO sys_permission (id, parent_id, code, name, type, path, component, icon, sort, visible, status) VALUES
(1, 0, 'system', '系统管理', 1, '/system', 'Layout', 'setting', 1, 1, 1);

-- 用户管理
INSERT INTO sys_permission (id, parent_id, code, name, type, path, component, icon, sort, visible, status) VALUES
(100, 1, 'system:user', '用户管理', 2, '/system/user', 'system/user/index', 'user', 1, 1, 1),
(101, 100, 'user:view', '查看用户', 3, '', '', '', 1, 1, 1),
(102, 100, 'user:create', '创建用户', 3, '', '', '', 2, 1, 1),
(103, 100, 'user:update', '更新用户', 3, '', '', '', 3, 1, 1),
(104, 100, 'user:delete', '删除用户', 3, '', '', '', 4, 1, 1),
(105, 100, 'user:reset-password', '重置密码', 3, '', '', '', 5, 1, 1),
(106, 100, 'user:assign-role', '分配角色', 3, '', '', '', 6, 1, 1);

-- 角色管理
INSERT INTO sys_permission (id, parent_id, code, name, type, path, component, icon, sort, visible, status) VALUES
(200, 1, 'system:role', '角色管理', 2, '/system/role', 'system/role/index', 'peoples', 2, 1, 1),
(201, 200, 'role:view', '查看角色', 3, '', '', '', 1, 1, 1),
(202, 200, 'role:create', '创建角色', 3, '', '', '', 2, 1, 1),
(203, 200, 'role:update', '更新角色', 3, '', '', '', 3, 1, 1),
(204, 200, 'role:delete', '删除角色', 3, '', '', '', 4, 1, 1),
(205, 200, 'role:assign-permission', '分配权限', 3, '', '', '', 5, 1, 1);

-- 权限管理
INSERT INTO sys_permission (id, parent_id, code, name, type, path, component, icon, sort, visible, status) VALUES
(300, 1, 'system:permission', '权限管理', 2, '/system/permission', 'system/permission/index', 'tree', 3, 1, 1),
(301, 300, 'permission:view', '查看权限', 3, '', '', '', 1, 1, 1),
(302, 300, 'permission:create', '创建权限', 3, '', '', '', 2, 1, 1),
(303, 300, 'permission:update', '更新权限', 3, '', '', '', 3, 1, 1),
(304, 300, 'permission:delete', '删除权限', 3, '', '', '', 4, 1, 1);

-- 日志管理
INSERT INTO sys_permission (id, parent_id, code, name, type, path, component, icon, sort, visible, status) VALUES
(400, 1, 'system:log', '日志管理', 2, '/system/log', 'system/log/index', 'log', 4, 1, 1),
(401, 400, 'log:view', '查看日志', 3, '', '', '', 1, 1, 1);

-- 初始化用户角色关联
INSERT INTO sys_user_role (id, user_id, role_id) VALUES
(1, 1, 1),  -- admin用户 -> 超级管理员角色
(2, 2, 2);  -- user用户 -> 普通用户角色

-- 初始化角色权限关联 (超级管理员拥有所有权限)
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(1, 1, 1),
(2, 1, 100), (3, 1, 101), (4, 1, 102), (5, 1, 103), (6, 1, 104), (7, 1, 105), (8, 1, 106),
(9, 1, 200), (10, 1, 201), (11, 1, 202), (12, 1, 203), (13, 1, 204), (14, 1, 205),
(15, 1, 300), (16, 1, 301), (17, 1, 302), (18, 1, 303), (19, 1, 304),
(20, 1, 400), (21, 1, 401);

-- 普通用户只有查看权限
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(22, 2, 1),
(23, 2, 100), (24, 2, 101),
(25, 2, 200), (26, 2, 201),
(27, 2, 300), (28, 2, 301);

