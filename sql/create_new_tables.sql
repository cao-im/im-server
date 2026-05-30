-- ============================================================
-- 快速建表脚本：创建新的好友系统表
-- 数据库: cao_im_db (localhost:3306, root/rootlocal)
-- 用途: 解决 "Table 'cao_im_db.im_friend_request' doesn't exist" 错误
--
-- 执行方式:
--   方式1: 命令行执行
--     mysql -u root -prootlocal cao_im_db < create_new_tables.sql
--
--   方式2: MySQL 客户端工具 (Navicat/Workbench/DataGrip)
--     打开此文件，选中所有 SQL，按 F5 或点击执行
--
--   方式3: 命令行登录后执行
--     mysql -u root -prootlocal
--     USE cao_im_db;
--     SOURCE /path/to/create_new_tables.sql;
-- ============================================================

USE cao_im_db;

-- ============================================================
-- 6. 好友申请表（存储好友申请流程）
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

    UNIQUE KEY uk_from_to (from_user_id, to_user_id),
    INDEX idx_to_user_id (to_user_id),
    INDEX idx_status (to_user_id, status),
    INDEX idx_source (source),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请表';

SELECT '✅ im_friend_request 表创建成功！' AS message;

-- ============================================================
-- 7. 联系人表（存储已建立的好友关系）
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

    UNIQUE KEY uk_user_contact (user_id, contact_user_id),
    INDEX idx_contact_user_id (contact_user_id),
    INDEX idx_group_id (user_id, group_id),
    INDEX idx_is_top (user_id, is_top DESC, update_time DESC),
    INDEX idx_update_time (user_id, update_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系人表';

SELECT '✅ im_contact 表创建成功！' AS message;

-- ============================================================
-- 验证表是否创建成功
-- ============================================================
SELECT 
    TABLE_NAME,
    TABLE_COMMENT,
    CREATE_TIME
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'cao_im_db' 
  AND TABLE_NAME IN ('im_friend_request', 'im_contact')
ORDER BY TABLE_NAME;

SELECT CONCAT('🎉 成功创建 ', COUNT(*), ' 张新表！') AS result
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'cao_im_db' 
  AND TABLE_NAME IN ('im_friend_request', 'im_contact');

-- ============================================================
-- 如果需要从旧表迁移数据，请执行 migrate_friend_to_new_tables.sql
-- ============================================================
