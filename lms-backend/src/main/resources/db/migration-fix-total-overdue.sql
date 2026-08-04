-- ============================================
-- 修正 total_overdue_amount 字段注释
--
-- total_overdue_amount 直接存储 GBase UNPD_INT_BAL 原始值
-- 展示层（导出/详情页）的"总逾期金额" = H+I+J 实时计算
-- 日期：2026-08-04
-- ============================================

-- MySQL:
ALTER TABLE loan_account MODIFY COLUMN total_overdue_amount DECIMAL(18,2) COMMENT '总逾期金额（GBase UNPD_INT_BAL原始值，展示层实时计算H+I+J）';

-- GaussDB:
-- COMMENT ON COLUMN loan_account.total_overdue_amount IS '总逾期金额（GBase UNPD_INT_BAL原始值，展示层实时计算H+I+J）';
