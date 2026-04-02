-- 紧急联系人拆分为姓名和手机号
-- 若 medication_profile 表已存在，执行此脚本添加新列
ALTER TABLE medication_profile ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(64) DEFAULT NULL COMMENT '紧急联系人姓名';
ALTER TABLE medication_profile ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(32) DEFAULT NULL COMMENT '紧急联系人手机号';
