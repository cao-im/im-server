-- 管理端依赖的 IM 库增量。新库执行 schema.sql 即可。
-- enabled 列已存在时，第一条 ALTER 会失败，忽略后继续执行后面的语句。
USE cao_im_db;

ALTER TABLE im_user
    ADD COLUMN enabled TINYINT NOT NULL DEFAULT 1 COMMENT '账号启停: 0-禁用, 1-启用' AFTER status;

CREATE TABLE IF NOT EXISTS im_sensitive_word (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '敏感词ID',
    word VARCHAR(50) NOT NULL COMMENT '敏感词',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1-启用, 0-停用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词';

CREATE TABLE IF NOT EXISTS im_system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(64) NOT NULL COMMENT '配置键',
    config_value VARCHAR(255) NOT NULL COMMENT '配置值',
    remark VARCHAR(200) DEFAULT '' COMMENT '说明',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';
