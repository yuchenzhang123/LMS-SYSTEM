-- ============================================
-- 修复：有催收记录但状态仍为 uncollected 的账户
--
-- 这些账户的状态应该为 collecting（催收中）
-- ============================================

-- 1. 将存在催收记录的 uncollected 账户批量改为 collecting
UPDATE loan_account
SET status = 'collecting',
    status_update_time = CURRENT_TIMESTAMP
WHERE status = 'uncollected'
  AND loan_account IN (
    SELECT DISTINCT loan_account FROM collection_record
  );

-- 2. 验证（可选）
-- SELECT la.loan_account, la.customer_name, la.status, COUNT(cr.record_id) AS record_count
-- FROM loan_account la
-- INNER JOIN collection_record cr ON la.loan_account = cr.loan_account
-- WHERE la.status = 'uncollected'
-- GROUP BY la.loan_account, la.customer_name, la.status
-- LIMIT 50;
