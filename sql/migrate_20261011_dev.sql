-- 已有库升级：给 im_user 增加密码列。
-- 新库直接执行 schema.sql 即可，不必再跑本脚本。
-- 如果列已存在，这条 ALTER 会失败，忽略即可。
USE cao_im_db;

ALTER TABLE im_user
    ADD COLUMN password VARCHAR(100) DEFAULT NULL COMMENT 'BCrypt密码，存的是客户端传来的口令' AFTER username;
