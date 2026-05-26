-- ============================================================
-- IM系统数据库初始化脚本
-- 数据库: cao_im_db
-- 版本: v4.0
-- 更新时间: 2026-05-26
-- 说明: 生产级IM数据库设计，支持私聊、群聊、消息撤回、已读回执等功能
-- ID策略: 全局使用数据库自增(AUTO_INCREMENT)
--       - 简单可靠，数据库自动管理
--       - 适合单机/中小规模部署场景
-- ============================================================

CREATE DATABASE IF NOT EXISTS cao_im_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cao_im_db;

-- ============================================================
-- 1. 用户表（IM用户，与业务用户分离）
-- ============================================================
CREATE TABLE IF NOT EXISTS im_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID(自增)',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT 'IM用户名',
    nickname VARCHAR(50) DEFAULT '' COMMENT '昵称',
    avatar VARCHAR(255) DEFAULT '' COMMENT '头像URL',
    signature VARCHAR(200) DEFAULT '' COMMENT '个性签名',
    gender TINYINT DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
    birthday DATE DEFAULT NULL COMMENT '生日',
    location VARCHAR(100) DEFAULT '' COMMENT '所在地',
    online_status TINYINT DEFAULT 0 COMMENT '在线状态: 0-离线, 1-在线, 2-忙碌, 3-隐身',
    last_online_time DATETIME DEFAULT NULL COMMENT '最后在线时间',
    status TINYINT DEFAULT 1 COMMENT '账号状态: 0-禁用, 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_username (username),
    INDEX idx_nickname (nickname),
    INDEX idx_online_status (online_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM用户表';

-- ============================================================
-- 2. 消息表（核心表，支持多种消息类型）
-- ============================================================
CREATE TABLE IF NOT EXISTS im_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID(自增)',
    msg_seq BIGINT NOT NULL COMMENT '消息序号(用于排序和去重)',
    from_id BIGINT NOT NULL COMMENT '发送者ID',
    to_id BIGINT DEFAULT NULL COMMENT '接收者ID(私聊时使用)',
    group_id BIGINT DEFAULT NULL COMMENT '群组ID(群聊时使用)',
    content TEXT NOT NULL COMMENT '消息内容',
    msg_type TINYINT DEFAULT 0 COMMENT '消息类型: 0-文本, 1-图片, 2-文件, 3-语音, 4-视频, 5-位置, 6-名片, 7-系统消息, 8-合并消息, 9-表情包',
    msg_status TINYINT DEFAULT 0 COMMENT '消息状态: 0-正常, 1-已撤回, 2-已删除',
    reply_msg_id BIGINT DEFAULT NULL COMMENT '引用/回复的消息ID',
    at_user_ids VARCHAR(500) DEFAULT '' COMMENT '@的用户ID列表(逗号分隔)',
    extra JSON DEFAULT NULL COMMENT '扩展信息(JSON格式,存储消息特有属性)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_msg_seq (msg_seq),
    INDEX idx_from_id (from_id),
    INDEX idx_to_id (to_id),
    INDEX idx_group_id (group_id),
    INDEX idx_create_time (create_time),
    INDEX idx_conversation (to_id, from_id, create_time),
    INDEX idx_group_conversation (group_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ============================================================
-- 3. 会话表（用户会话列表）
-- ============================================================
CREATE TABLE IF NOT EXISTS im_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID(自增)',
    user_id BIGINT NOT NULL COMMENT '用户ID(会话所属者)',
    target_id BIGINT NOT NULL COMMENT '目标ID(对方用户ID或群组ID)',
    conversation_type TINYINT NOT NULL COMMENT '会话类型: 1-私聊, 2-群聊',
    last_msg_id BIGINT DEFAULT NULL COMMENT '最后一条消息ID',
    last_message VARCHAR(500) DEFAULT '' COMMENT '最后一条消息内容(冗余字段,提升列表查询性能)',
    last_msg_type TINYINT DEFAULT 0 COMMENT '最后一条消息类型',
    last_msg_time DATETIME DEFAULT NULL COMMENT '最后一条消息时间',
    unread_count INT DEFAULT 0 COMMENT '未读消息数',
    is_top TINYINT DEFAULT 0 COMMENT '是否置顶: 0-否, 1-是',
    is_mute TINYINT DEFAULT 0 COMMENT '是否免打扰: 0-否, 1-是',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除: 0-否, 1-是(仅删除会话,不删除消息)',
    draft_content TEXT DEFAULT NULL COMMENT '草稿内容',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_target (user_id, target_id, conversation_type),
    INDEX idx_user_id (user_id),
    INDEX idx_update_time (user_id, update_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- ============================================================
-- 4. 群组表
-- ============================================================
CREATE TABLE IF NOT EXISTS im_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群组ID(自增)',
    name VARCHAR(100) NOT NULL COMMENT '群组名称',
    avatar VARCHAR(255) DEFAULT '' COMMENT '群组头像URL',
    introduction VARCHAR(500) DEFAULT '' COMMENT '群公告',
    notice TEXT DEFAULT NULL COMMENT '群简介/群描述',
    owner_id BIGINT NOT NULL COMMENT '群主ID',
    max_members INT DEFAULT 500 COMMENT '最大成员数',
    join_type TINYINT DEFAULT 0 COMMENT '入群方式: 0-自由加入, 1-需要验证, 2-禁止加入',
    mute_all TINYINT DEFAULT 0 COMMENT '全员禁言: 0-否, 1-是',
    status TINYINT DEFAULT 1 COMMENT '群组状态: 0-已解散, 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_owner_id (owner_id),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- ============================================================
-- 5. 群成员表
-- ============================================================
CREATE TABLE IF NOT EXISTS im_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员记录ID(自增)',
    group_id BIGINT NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    nickname VARCHAR(50) DEFAULT '' COMMENT '群内昵称',
    avatar VARCHAR(255) DEFAULT '' COMMENT '群内头像(可选,覆盖默认头像)',
    role TINYINT DEFAULT 0 COMMENT '角色: 0-普通成员, 1-管理员, 2-群主',
    mute TINYINT DEFAULT 0 COMMENT '是否被禁言: 0-否, 1-是',
    last_read_msg_id BIGINT DEFAULT 0 COMMENT '已读消息ID(用于群聊已读回执)',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    invite_user_id BIGINT DEFAULT NULL COMMENT '邀请人ID',
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role (group_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- ============================================================
-- 6. 好友关系表
-- ============================================================
CREATE TABLE IF NOT EXISTS im_friend (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '好友关系ID(自增)',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    friend_id BIGINT NOT NULL COMMENT '好友ID',
    remark VARCHAR(50) DEFAULT '' COMMENT '好友备注',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-待验证, 1-已同意, 2-已拒绝, 3-已删除',
    apply_message VARCHAR(200) DEFAULT '' COMMENT '申请/验证消息',
    source TINYINT DEFAULT 0 COMMENT '添加来源: 0-搜索, 1-群聊, 2-二维码, 3-名片分享',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    agree_time DATETIME DEFAULT NULL COMMENT '同意时间',
    UNIQUE KEY uk_user_friend (user_id, friend_id),
    INDEX idx_friend_id (friend_id),
    INDEX idx_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友表';

-- ============================================================
-- 7. 黑名单表
-- ============================================================
CREATE TABLE IF NOT EXISTS im_blacklist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID(自增)',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    blocked_user_id BIGINT NOT NULL COMMENT '被拉黑的用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '拉黑时间',
    UNIQUE KEY uk_user_blocked (user_id, blocked_user_id),
    INDEX idx_blocked_user (blocked_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='黑名单表';

-- ============================================================
-- 8. 消息附件表（文件、图片、语音、视频等）
-- ============================================================
CREATE TABLE IF NOT EXISTS im_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '附件ID(自增)',
    msg_id BIGINT NOT NULL COMMENT '关联的消息ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_url VARCHAR(500) NOT NULL COMMENT '文件访问URL',
    file_path VARCHAR(500) DEFAULT '' COMMENT '文件存储路径',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型(MIME类型)',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    file_ext VARCHAR(20) DEFAULT '' COMMENT '文件扩展名',
    thumbnail_url VARCHAR(500) DEFAULT '' COMMENT '缩略图URL(图片/视频专用)',
    width INT DEFAULT NULL COMMENT '宽度(图片/视频专用)',
    height INT DEFAULT NULL COMMENT '高度(图片/视频专用)',
    duration INT DEFAULT NULL COMMENT '时长(秒,音频/视频专用)',
    extra JSON DEFAULT NULL COMMENT '扩展信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    INDEX idx_msg_id (msg_id),
    INDEX idx_file_type (file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息附件表';

-- ============================================================
-- 9. 消息已读回执表（主要用于群聊场景）
-- ============================================================
CREATE TABLE IF NOT EXISTS im_message_read (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID(自增)',
    msg_id BIGINT NOT NULL COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '已读用户ID',
    group_id BIGINT DEFAULT NULL COMMENT '群组ID(群聊消息必填)',
    read_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '读取时间',
    UNIQUE KEY uk_msg_user (msg_id, user_id),
    INDEX idx_user_read (user_id, read_time),
    INDEX idx_group_read (group_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息已读回执表';

-- ============================================================
-- 10. 操作日志表（安全审计）
-- ============================================================
CREATE TABLE IF NOT EXISTS im_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID(自增)',
    user_id BIGINT NOT NULL COMMENT '操作用户ID',
    operation VARCHAR(50) NOT NULL COMMENT '操作类型: LOGIN/LOGOUT/SEND_MSG/DELETE_MSG/CREATE_GROUP/KICK_MEMBER等',
    target_type VARCHAR(20) DEFAULT NULL COMMENT '目标类型: USER/GROUP/MESSAGE/FRIEND',
    target_id BIGINT DEFAULT NULL COMMENT '目标ID',
    detail TEXT DEFAULT NULL COMMENT '操作详情(JSON格式)',
    ip VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT '客户端信息',
    result TINYINT DEFAULT 1 COMMENT '执行结果: 0-失败, 1-成功',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_user_id (user_id),
    INDEX idx_operation (operation),
    INDEX idx_create_time (create_time),
    INDEX idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================================
-- 11. 群消息免打扰用户表（细粒度控制）
-- ============================================================
CREATE TABLE IF NOT EXISTS im_group_mute (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID(自增)',
    group_id BIGINT NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '设置时间',
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息免打扰表';
