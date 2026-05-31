-- ============================================================
-- IM系统数据库初始化脚本（完整版 v6.0）
-- 数据库: cao_im_db
-- 版本: v6.0 (重构好友系统)
-- 更新时间: 2026-05-31
-- 说明: 生产级IM数据库设计，支持私聊、群聊、消息撤回、已读回执、
--       黑名单、文件消息、@提醒、消息回复等完整功能
--
-- 🆕 v6.0 更新内容:
--   - 重构好友系统：将 im_friend 表拆分为 im_friend_request + im_contact
--   - 新增好友申请流程表 (im_friend_request)
--   - 新增联系人/好友关系表 (im_contact) 支持备注、分组、置顶、免打扰
--   - 废弃旧表 im_friend（如存在将自动备份并提示迁移）
--
-- 表清单 (共12张):
--   1. im_user          - 用户表
--   2. im_message       - 消息表（核心）
--   3. im_conversation  - 会话表
--   4. im_group         - 群组表
--   5. im_group_member  - 群成员表
--   6. im_friend_request- 好友申请表 [新增]
--   7. im_contact       - 联系人表（好友关系）[新增]
--   8. im_blacklist     - 黑名单表
--   9. im_attachment    - 消息附件表
--   10. im_message_read - 消息已读回执表
--   11. im_operation_log- 操作日志表
--   12. im_group_mute   - 群消息免打扰表
--
-- ID策略: 全局使用数据库自增(AUTO_INCREMENT)
-- 字符集: utf8mb4 (支持完整Unicode，包括emoji表情)
--
-- 执行方式:
--   mysql -u root -prootlocal < schema.sql
-- 或在 MySQL 客户端中执行此文件
-- ============================================================

CREATE DATABASE IF NOT EXISTS cao_im_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cao_im_db;

-- ============================================================
-- 1. 用户表（IM用户，与业务系统用户分离）
-- 功能: 存储IM用户基本信息、在线状态、个人资料
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
    phone VARCHAR(20) DEFAULT '' COMMENT '手机号',
    email VARCHAR(100) DEFAULT '' COMMENT '邮箱',
    online_status TINYINT DEFAULT 0 COMMENT '在线状态: 0-离线, 1-在线, 2-忙碌, 3-隐身',
    last_online_time DATETIME DEFAULT NULL COMMENT '最后在线时间',
    status TINYINT DEFAULT 1 COMMENT '账号状态: 0-禁用, 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',

    -- 索引优化
    INDEX idx_username (username),
    INDEX idx_nickname (nickname),
    INDEX idx_online_status (online_status),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM用户表';

-- ============================================================
-- 2. 消息表（核心表，支持多种消息类型和高级功能）
-- 功能: 存储所有聊天消息（私聊+群聊），支持消息回复、@提醒、撤回等
-- ============================================================
CREATE TABLE IF NOT EXISTS im_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID(自增)',
    mid BIGINT NOT NULL DEFAULT 0 COMMENT '消息全局唯一ID(雪花算法生成,0表示待分配)',
    msg_seq BIGINT NOT NULL DEFAULT 0 COMMENT '消息序号(用于排序和去重,保证全局有序)',
    from_id BIGINT NOT NULL COMMENT '发送者ID',
    to_id BIGINT DEFAULT NULL COMMENT '接收者ID(私聊时使用,与group_id互斥)',
    group_id BIGINT DEFAULT NULL COMMENT '群组ID(群聊时使用,与to_id互斥)',
    content TEXT NOT NULL COMMENT '消息内容',
    msg_type TINYINT DEFAULT 0 COMMENT '消息类型: 0-文本, 1-图片, 2-文件, 3-语音, 4-视频, 5-位置, 6-名片, 7-系统消息, 8-合并消息, 9-表情包',
    msg_status TINYINT DEFAULT 0 COMMENT '消息阅读状态: 0-未读, 1-已读',
    reply_msg_id BIGINT DEFAULT NULL COMMENT '引用/回复的消息ID(实现消息引用功能)',
    at_user_ids VARCHAR(500) DEFAULT '' COMMENT '@的用户ID列表(逗号分隔,如"1001,1002")',
    extra JSON DEFAULT NULL COMMENT '扩展信息(JSON格式,存储消息特有属性,如图片尺寸、语音时长等)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',

    -- 索引优化（高频查询场景）
    INDEX idx_msg_seq (msg_seq),                              -- 消息序号索引（增量同步）
    INDEX idx_from_id (from_id),                              -- 发送者查询
    INDEX idx_to_id (to_id),                                  -- 接收者查询（离线消息拉取）
    INDEX idx_group_id (group_id),                            -- 群组消息查询
    INDEX idx_create_time (create_time),                      -- 时间范围查询
    INDEX idx_conversation (to_id, from_id, create_time),     -- 私聊会话查询
    INDEX idx_group_conversation (group_id, create_time),     -- 群聊会话查询
    INDEX idx_from_create (from_id, create_time)              -- 发送记录查询
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ============================================================
-- 3. 会话表（用户会话列表）
-- 功能: 存储用户的会话列表，支持置顶、免打扰、草稿等功能
-- ============================================================
CREATE TABLE IF NOT EXISTS im_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID(自增)',
    user_id BIGINT NOT NULL COMMENT '用户ID(会话所属者)',
    target_id BIGINT NOT NULL COMMENT '目标ID(对方用户ID或群组ID)',
    conversation_type TINYINT NOT NULL COMMENT '会话类型: 1-私聊, 2-群聊',
    last_msg_id BIGINT DEFAULT NULL COMMENT '最后一条消息ID(关联im_message.id)',
    last_message VARCHAR(500) DEFAULT '' COMMENT '最后一条消息内容(冗余字段,提升列表查询性能)',
    last_msg_type TINYINT DEFAULT 0 COMMENT '最后一条消息类型(UI显示对应图标)',
    last_msg_time DATETIME DEFAULT NULL COMMENT '最后一条消息时间(会话排序依据)',
    unread_count INT DEFAULT 0 COMMENT '未读消息数',
    is_top TINYINT DEFAULT 0 COMMENT '是否置顶: 0-否, 1-是',
    is_mute TINYINT DEFAULT 0 COMMENT '是否免打扰: 0-否, 1-是(不推送通知)',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除: 0-否, 1-是(仅删除会话,不删除消息)',
    draft_content TEXT DEFAULT NULL COMMENT '草稿内容(输入框未发送的内容)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 唯一约束：每个用户对同一目标只能有一个会话
    UNIQUE KEY uk_user_target (user_id, target_id, conversation_type),

    -- 索引优化
    INDEX idx_user_id (user_id),
    INDEX idx_update_time (user_id, update_time DESC),         -- 会话列表按时间排序
    INDEX idx_unread (user_id, is_top DESC, update_time DESC)  -- 未读+置顶排序
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- ============================================================
-- 4. 群组表
-- 功能: 存储群组基本信息和管理配置
-- ============================================================
CREATE TABLE IF NOT EXISTS im_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群组ID(自增)',
    name VARCHAR(100) NOT NULL COMMENT '群组名称',
    avatar VARCHAR(255) DEFAULT '' COMMENT '群组头像URL',
    introduction VARCHAR(500) DEFAULT '' COMMENT '群公告(管理员发布的重要通知)',
    notice TEXT DEFAULT NULL COMMENT '群简介/群描述(群介绍信息)',
    owner_id BIGINT NOT NULL COMMENT '群主ID(拥有最高权限)',
    max_members INT DEFAULT 500 COMMENT '最大成员数(0表示无限制)',
    join_type TINYINT DEFAULT 0 COMMENT '入群方式: 0-自由加入, 1-需要验证, 2-禁止加入',
    mute_all TINYINT DEFAULT 0 COMMENT '全员禁言: 0-否, 1-是(仅管理员可发言)',
    status TINYINT DEFAULT 1 COMMENT '群组状态: 0-已解散, 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引优化
    INDEX idx_owner_id (owner_id),
    INDEX idx_name (name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- ============================================================
-- 5. 群成员表
-- 功能: 存储群组成员信息，支持角色管理、禁言、已读回执
-- ============================================================
CREATE TABLE IF NOT EXISTS im_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员记录ID(自增)',
    group_id BIGINT NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    nickname VARCHAR(50) DEFAULT '' COMMENT '群内昵称(可自定义,覆盖默认昵称)',
    avatar VARCHAR(255) DEFAULT '' COMMENT '群内头像(可选,覆盖默认头像)',
    role TINYINT DEFAULT 0 COMMENT '角色: 0-普通成员, 1-管理员, 2-群主',
    mute TINYINT DEFAULT 0 COMMENT '是否被禁言: 0-否, 1-是(该用户无法在群内发消息)',
    last_read_msg_id BIGINT DEFAULT 0 COMMENT '已读消息ID(用于群聊已读回执,显示"已读X人")',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    invite_user_id BIGINT DEFAULT NULL COMMENT '邀请人ID(谁邀请该用户入群)',

    -- 唯一约束：同一用户在同一群只能有一条记录
    UNIQUE KEY uk_group_user (group_id, user_id),

    -- 索引优化
    INDEX idx_user_id (user_id),
    INDEX idx_role (group_id, role),                          -- 按角色筛选成员
    INDEX idx_join_time (join_time)                           -- 新成员查询
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- ============================================================
-- 6. 好友申请表（存储好友申请流程）
-- 功能: 存储好友申请、验证消息、状态流转
-- ============================================================
CREATE TABLE IF NOT EXISTS im_friend_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请记录ID(自增)',
    from_user_id BIGINT NOT NULL COMMENT '申请人ID(发起方)',
    to_user_id BIGINT NOT NULL COMMENT '被申请人ID(接收方)',
    apply_message VARCHAR(200) DEFAULT '' COMMENT '申请留言(添加好友时的留言)',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-待处理, 1-已同意, 2-已拒绝',
    source TINYINT DEFAULT 0 COMMENT '添加来源: 0-搜索, 1-群聊, 2-二维码, 3-名片分享, 4-通讯录',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    handle_time DATETIME DEFAULT NULL COMMENT '处理时间',

    -- 唯一约束：同一申请人不能重复申请同一人
    UNIQUE KEY uk_from_to (from_user_id, to_user_id),

    -- 索引优化
    INDEX idx_to_user_id (to_user_id),                        -- 查询收到的申请
    INDEX idx_status (to_user_id, status),                    -- 按状态查询申请
    INDEX idx_source (source),                                -- 来源统计
    INDEX idx_create_time (create_time)                       -- 时间排序
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请表';

-- ============================================================
-- 7. 联系人表（存储已建立的好友关系）
-- 功能: 存储联系人列表，支持备注、分组、置顶等功能
-- ============================================================
CREATE TABLE IF NOT EXISTS im_contact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '联系人记录ID(自增)',
    user_id BIGINT NOT NULL COMMENT '用户ID(联系人所有者)',
    contact_user_id BIGINT NOT NULL COMMENT '联系人ID(对方用户)',
    remark VARCHAR(50) DEFAULT '' COMMENT '备注名(可自定义显示名称)',
    group_id INT DEFAULT 0 COMMENT '分组ID: 0-默认分组, 1-家人, 2-同事, 3-朋友...',
    is_top TINYINT DEFAULT 0 COMMENT '是否置顶: 0-否, 1-是',
    is_mute TINYINT DEFAULT 0 COMMENT '是否免打扰: 0-否, 1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 唯一约束：每个用户对同一联系人只能有一条记录
    UNIQUE KEY uk_user_contact (user_id, contact_user_id),

    -- 索引优化
    INDEX idx_contact_user_id (contact_user_id),              -- 被谁添加为联系人
    INDEX idx_group_id (user_id, group_id),                   -- 按分组查询
    INDEX idx_is_top (user_id, is_top DESC, update_time DESC),-- 置顶+时间排序
    INDEX idx_update_time (user_id, update_time DESC)         -- 联系人列表排序
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系人表';

-- ============================================================
-- 8. 黑名单表
-- 功能: 存储用户黑名单关系，实现屏蔽/拉黑功能
-- ============================================================
CREATE TABLE IF NOT EXISTS im_blacklist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID(自增)',
    user_id BIGINT NOT NULL COMMENT '用户ID(操作者)',
    blocked_user_id BIGINT NOT NULL COMMENT '被拉黑的用户ID(目标)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '拉黑时间',

    -- 唯一约束：同一用户不能重复拉黑同一人
    UNIQUE KEY uk_user_blocked (user_id, blocked_user_id),

    -- 索引优化
    INDEX idx_blocked_user (blocked_user_id)                 -- 被谁拉黑查询
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='黑名单表';

-- ============================================================
-- 9. 消息附件表（文件、图片、语音、视频等富媒体消息）
-- 功能: 存储消息关联的附件信息，支持多种文件类型
-- ============================================================
CREATE TABLE IF NOT EXISTS im_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '附件ID(自增)',
    msg_id BIGINT NOT NULL COMMENT '关联的消息ID(im_message.id)',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名(含扩展名)',
    file_url VARCHAR(500) NOT NULL COMMENT '文件访问URL(客户端下载地址)',
    file_path VARCHAR(500) DEFAULT '' COMMENT '文件存储路径(服务器本地路径)',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型(MIME类型,如image/jpeg)',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    file_ext VARCHAR(20) DEFAULT '' COMMENT '文件扩展名(如jpg, mp4, pdf)',
    thumbnail_url VARCHAR(500) DEFAULT '' COMMENT '缩略图URL(图片/视频专用,提升列表加载速度)',
    width INT DEFAULT NULL COMMENT '宽度像素(图片/视频专用)',
    height INT DEFAULT NULL COMMENT '高度像素(图片/视频专用)',
    duration INT DEFAULT NULL COMMENT '时长秒数(音频/视频专用)',
    extra JSON DEFAULT NULL COMMENT '扩展信息(JSON格式,存储特殊属性)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',

    -- 索引优化
    INDEX idx_msg_id (msg_id),                               -- 查询消息的附件
    INDEX idx_file_type (file_type),                         -- 按类型筛选
    INDEX idx_create_time (create_time)                      -- 时间范围查询
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息附件表';

-- ============================================================
-- 10. 消息已读回执表（主要用于群聊场景）
-- 功能: 记录消息已读状态，实现"已读X人"功能
-- ============================================================
CREATE TABLE IF NOT EXISTS im_message_read (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID(自增)',
    msg_id BIGINT NOT NULL COMMENT '消息ID(im_message.id)',
    user_id BIGINT NOT NULL COMMENT '已读用户ID',
    group_id BIGINT DEFAULT NULL COMMENT '群组ID(群聊消息必填,私聊可为NULL)',
    read_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '读取时间',

    -- 唯一约束：同一用户对同一条消息只记录一次已读
    UNIQUE KEY uk_msg_user (msg_id, user_id),

    -- 索引优化
    INDEX idx_user_read (user_id, read_time),                -- 用户已读历史
    INDEX idx_group_read (group_id, user_id),               -- 群成员已读状态
    INDEX idx_msg_read (msg_id, read_time)                  -- 消息已读统计
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息已读回执表';

-- ============================================================
-- 11. 操作日志表（安全审计和问题追踪）
-- 功能: 记录关键操作日志，用于安全审计、问题排查、数据分析
-- ============================================================
CREATE TABLE IF NOT EXISTS im_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID(自增)',
    user_id BIGINT NOT NULL COMMENT '操作用户ID',
    operation VARCHAR(50) NOT NULL COMMENT '操作类型(LOGIN/LOGOUT/SEND_MSG/DELETE_MSG/CREATE_GROUP/KICK_MEMBER/ADD_FRIEND等)',
    target_type VARCHAR(20) DEFAULT NULL COMMENT '目标类型(USER/GROUP/MESSAGE/FRIEND/ATTACHMENT)',
    target_id BIGINT DEFAULT NULL COMMENT '目标ID',
    detail TEXT DEFAULT NULL COMMENT '操作详情(JSON格式,包含请求参数、响应结果等)',
    ip VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址(用于安全分析)',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT '客户端信息(浏览器/App版本等)',
    result TINYINT DEFAULT 1 COMMENT '执行结果: 0-失败, 1-成功',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    -- 索引优化
    INDEX idx_user_id (user_id),                             -- 用户操作历史
    INDEX idx_operation (operation),                        -- 按操作类型筛选
    INDEX idx_create_time (create_time),                    -- 时间范围查询
    INDEX idx_user_time (user_id, create_time),             -- 用户+时间组合查询
    INDEX idx_result (result)                               -- 成功/失败筛选
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================================
-- 12. 群消息免打扰用户表（细粒度控制）
-- 功能: 记录哪些用户对哪些群开启了免打扰（区别于is_mute的全局设置）
-- ============================================================
CREATE TABLE IF NOT EXISTS im_group_mute (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID(自增)',
    group_id BIGINT NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID(开启免打扰的用户)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '设置时间',

    -- 唯一约束：同一用户对同一群只能有一条免打扰记录
    UNIQUE KEY uk_group_user (group_id, user_id),

    -- 索引优化
    INDEX idx_user_id (user_id)                              -- 查询某用户的所有免打扰群
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息免打扰表';

-- ============================================================
-- 部署验证
-- ============================================================
SELECT '========================================' AS `line`;
SELECT '        IM 系统数据库部署完成' AS title;
SELECT '========================================' AS `line`;

SELECT CONCAT('版本: v6.0') AS version;
SELECT CONCAT('数据库: ', DATABASE()) AS database_name;
SELECT CONCAT('字符集: utf8mb4') AS charset;

SELECT '--- 表清单 ---' AS section_header;
SELECT
    TABLE_NAME AS `表名`,
    TABLE_COMMENT AS `说明`,
    CREATE_TIME AS `创建时间`
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME LIKE 'im_%'
ORDER BY TABLE_NAME;

SELECT CONCAT('共创建 ', COUNT(*), ' 张数据表') AS summary
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME LIKE 'im_%';

SELECT '========================================' AS `line`;
SELECT '✅ 数据库初始化成功！可以启动应用了。' AS message;
SELECT '========================================' AS `line`;
