-- ============================================================
-- 数据迁移脚本：从 im_friend 表迁移到新表结构
-- 目标: 将 im_friend 表数据拆分到 im_friend_request 和 im_contact
-- 执行时机: 在创建新表结构后执行此脚本
-- 注意: 请在执行前备份原数据！
-- ============================================================

USE cao_im_db;

-- ============================================================
-- 步骤 1: 迁移好友申请记录（所有状态）
-- 从 im_friend 表中提取申请相关字段到 im_friend_request 表
-- ============================================================
INSERT INTO im_friend_request (id, from_user_id, to_user_id, apply_message, status, source, create_time, handle_time)
SELECT 
    id,
    user_id as from_user_id,
    friend_id as to_user_id,
    apply_message,
    CASE status 
        WHEN 0 THEN 0  -- 待验证 -> 待处理
        WHEN 1 THEN 1  -- 已同意 -> 已同意
        WHEN 2 THEN 2  -- 已拒绝 -> 已拒绝
        WHEN 3 THEN 2  -- 已删除 -> 已拒绝(作为历史记录)
        ELSE 0
    END as status,
    source,
    create_time,
    agree_time as handle_time
FROM im_friend
WHERE NOT EXISTS (
    SELECT 1 FROM im_friend_request WHERE id = im_friend.id
);

-- ============================================================
-- 步骤 2: 迁移已建立的好友关系到联系人表
-- 只迁移状态为"已同意"(status=1)的记录
-- 双向存储: 每条好友关系生成两条联系人记录
-- ============================================================
INSERT INTO im_contact (user_id, contact_user_id, remark, group_id, is_top, is_mute, create_time)
SELECT 
    user_id,
    friend_id as contact_user_id,
    remark,
    0 as group_id,     -- 默认分组
    0 as is_top,       -- 默认不置顶
    0 as is_mute,      -- 默认不免打扰
    COALESCE(agree_time, create_time) as create_time  -- 使用同意时间或创建时间
FROM im_friend
WHERE status = 1  -- 只迁移已同意的
  AND NOT EXISTS (
      SELECT 1 FROM im_contact 
      WHERE user_id = im_friend.user_id 
      AND contact_user_id = im_friend.friend_id
  );

-- ============================================================
-- 步骤 3: 验证迁移结果
-- ============================================================
SELECT '=== 迁移统计 ===' AS info;

SELECT CONCAT('原表 im_friend 总记录数: ', COUNT(*)) AS statistic 
FROM im_friend;

SELECT CONCAT('新表 im_friend_request 记录数: ', COUNT(*)) AS statistic 
FROM im_friend_request;

SELECT CONCAT('新表 im_contact 记录数: ', COUNT(*)) AS statistic 
FROM im_contact;

-- 验证已同意的好友关系是否完整迁移
SELECT CONCAT('原表中已同意的关系数: ', COUNT(*)) AS statistic 
FROM im_friend WHERE status = 1;

SELECT CONCAT('联系人表中应该有的记录数(双向): ', COUNT(*) * 2) AS statistic 
FROM im_friend WHERE status = 1;

-- ============================================================
-- 步骤 4: 数据一致性检查（可选）
-- 检查是否有遗漏的数据
-- ============================================================
SELECT '=== 数据一致性检查 ===' AS check_type;

-- 检查是否有已同意但未迁移到联系人表的记录
SELECT f.id, f.user_id, f.friend_id, f.status
FROM im_friend f
LEFT JOIN im_contact c ON f.user_id = c.user_id AND f.friend_id = c.contact_user_id
WHERE f.status = 1 AND c.id IS NULL
LIMIT 10;

-- ============================================================
-- 步骤 5: 清理建议（执行前请确认数据无误）
-- 以下语句会删除原表，请确认后再取消注释执行！
-- ============================================================

-- 备份原表（可选）
-- CREATE TABLE im_friend_backup_20260531 AS SELECT * FROM im_friend;

-- 删除原表（确认数据迁移成功后执行）
-- DROP TABLE IF EXISTS im_friend;

SELECT '✅ 数据迁移完成！' AS message;
SELECT '⚠️  请验证数据完整性后，再决定是否删除原表 im_friend' AS warning;
