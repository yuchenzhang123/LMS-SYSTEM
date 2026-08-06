-- ============================================
-- 修复 total_overdue_amount：不再使用 GBase UNPD_INT_BAL，改为三列相加
-- 即 total_overdue_amount = overdue_principal + overdue_interest + overdue_penalty (H+I+J)
-- 日期：2026-08-04
-- ============================================

-- 1. 修正字段注释
-- MySQL:
ALTER TABLE loan_account MODIFY COLUMN total_overdue_amount DECIMAL(18,2) COMMENT '总逾期金额（逾期本金+逾期利息+逾期罚息）';

-- GaussDB:
-- COMMENT ON COLUMN loan_account.total_overdue_amount IS '总逾期金额（逾期本金+逾期利息+逾期罚息）';

-- 2. 修正存量数据
-- MySQL:
UPDATE loan_account
SET total_overdue_amount =
    COALESCE(overdue_principal, 0) +
    COALESCE(overdue_interest, 0) +
    COALESCE(overdue_penalty, 0);

-- GaussDB:
-- UPDATE loan_account
-- SET total_overdue_amount =
--     COALESCE(overdue_principal, 0) +
--     COALESCE(overdue_interest, 0) +
--     COALESCE(overdue_penalty, 0);

-- 3. 验证（可选）
-- SELECT loan_account, overdue_principal, overdue_interest, overdue_penalty,
--        total_overdue_amount AS old_value,
--        (COALESCE(overdue_principal,0) + COALESCE(overdue_interest,0) + COALESCE(overdue_penalty,0)) AS new_value
-- FROM loan_account
-- WHERE total_overdue_amount != COALESCE(overdue_principal,0) + COALESCE(overdue_interest,0) + COALESCE(overdue_penalty,0)
-- LIMIT 20;
