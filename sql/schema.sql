-- IM系统数据库初始化脚本
-- 数据库: cao_im_db

CREATE DATABASE IF NOT EXISTS cao_im_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cao_im_db;

-- 用户表
CREATE TABLE IF NOT EXISTS im_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) DEFAULT '' COMMENT '昵称',
    avatar VARCHAR(255) DEFAULT '' COMMENT '头像URL',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-离线, 1-在线, 2-忙碌',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 消息表
CREATE TABLE IF NOT EXISTS im_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    from_id BIGINT NOT NULL COMMENT '发送者ID',
    to_id BIGINT DEFAULT NULL COMMENT '接收者ID(私聊)',
    group_id BIGINT DEFAULT NULL COMMENT '群组ID(群聊)',
    content TEXT NOT NULL COMMENT '消息内容',
    msg_type TINYINT DEFAULT 0 COMMENT '消息类型: 0-文本, 1-图片, 2-文件, 3-语音, 4-视频',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-未读, 1-已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_from_id (from_id),
    INDEX idx_to_id (to_id),
    INDEX idx_group_id (group_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 会话表
CREATE TABLE IF NOT EXISTS im_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    target_id BIGINT NOT NULL COMMENT '目标ID(用户或群组)',
    conversation_type TINYINT NOT NULL COMMENT '会话类型: 1-私聊, 2-群聊',
    last_message TEXT COMMENT '最后一条消息内容',
    unread_count INT DEFAULT 0 COMMENT '未读消息数',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_target_id (target_id),
    INDEX idx_user_target (user_id, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- 群组表
CREATE TABLE IF NOT EXISTS im_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群组ID',
    name VARCHAR(100) NOT NULL COMMENT '群组名称',
    avatar VARCHAR(255) DEFAULT '' COMMENT '群组头像',
    owner_id BIGINT NOT NULL COMMENT '群主ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_owner_id (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- 群成员表
CREATE TABLE IF NOT EXISTS im_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员记录ID',
    group_id BIGINT NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role TINYINT DEFAULT 0 COMMENT '角色: 0-普通成员, 1-管理员, 2-群主',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- 好友表
CREATE TABLE IF NOT EXISTS im_friend (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '好友关系ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    friend_id BIGINT NOT NULL COMMENT '好友ID',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-待验证, 1-已同意, 2-已拒绝, 3-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_friend (user_id, friend_id),
    INDEX idx_friend_id (friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友表';
