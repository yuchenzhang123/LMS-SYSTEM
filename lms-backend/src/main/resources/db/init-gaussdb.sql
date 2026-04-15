-- ============================================
-- LMS催收管理系统 数据库初始化脚本
-- 数据库: GaussDB (PostgreSQL兼容)
-- ============================================

-- 创建数据库（如果不存在，需要管理员权限）
-- CREATE DATABASE lms_db WITH ENCODING='UTF8';
-- \c lms_db

-- ============================================
-- 1. 贷款账户表 (loan_account)
-- ============================================
CREATE TABLE IF NOT EXISTS loan_account (
    loan_account VARCHAR(32) PRIMARY KEY,
    customer_id VARCHAR(32) NOT NULL,
    customer_name VARCHAR(100),
    phone VARCHAR(20),
    product_code VARCHAR(32),
    product_name VARCHAR(100),
    loan_date DATE,
    loan_term INT,
    overdue_days INT DEFAULT 0,
    contract_amount DECIMAL(18,2),
    loan_balance DECIMAL(18,2),
    unexpired_principal DECIMAL(18,2),
    overdue_principal DECIMAL(18,2),
    overdue_interest DECIMAL(18,2),
    overdue_penalty DECIMAL(18,2),
    total_overdue_amount DECIMAL(18,2),
    status VARCHAR(20) DEFAULT 'uncollected',
    status_update_time TIMESTAMP NULL,
    gbase_sync_time TIMESTAMP NULL,
    gbase_raw_data TEXT,
    extra_data TEXT,
    branch_code VARCHAR(20),
    branch_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_loan_account_customer_id ON loan_account(customer_id);
CREATE INDEX IF NOT EXISTS idx_loan_account_status ON loan_account(status);
CREATE INDEX IF NOT EXISTS idx_loan_account_overdue_days ON loan_account(overdue_days);
CREATE INDEX IF NOT EXISTS idx_loan_account_product_code ON loan_account(product_code);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_loan_account_branch_code ON loan_account(branch_code);

-- 添加注释
COMMENT ON TABLE loan_account IS '贷款账户表（从GBase转换存储）';
COMMENT ON COLUMN loan_account.loan_account IS '贷款账号（业务主键）';
COMMENT ON COLUMN loan_account.customer_id IS '客户ID';
COMMENT ON COLUMN loan_account.customer_name IS '客户姓名（冗余，方便查询）';
COMMENT ON COLUMN loan_account.branch_code IS '分支行号（LOAN_BRANCH_NO）';
COMMENT ON COLUMN loan_account.branch_name IS '分支行名称（LOAN_BRANCH_NAME）';
COMMENT ON COLUMN loan_account.status IS '状态: uncollected/collecting/completed';
COMMENT ON COLUMN loan_account.is_deleted IS '逻辑删除标记';

-- ============================================
-- 2. 催收记录表 (collection_record)
-- ============================================
CREATE TABLE IF NOT EXISTS collection_record (
    record_id VARCHAR(32) PRIMARY KEY,
    loan_account VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    customer_name VARCHAR(100),
    target_type VARCHAR(20),
    target_name VARCHAR(100),
    actual_collection_time TIMESTAMP NULL,
    method VARCHAR(20) NOT NULL,
    method_text VARCHAR(50),
    result VARCHAR(500),
    operator_id VARCHAR(32),
    operator_name VARCHAR(100),
    operate_time TIMESTAMP NULL,
    remark VARCHAR(500),
    material_type VARCHAR(20),
    material_name VARCHAR(200),
    material_url VARCHAR(500),
    extra_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_record_loan_account ON collection_record(loan_account);
CREATE INDEX IF NOT EXISTS idx_record_customer_id ON collection_record(customer_id);
CREATE INDEX IF NOT EXISTS idx_record_operate_time ON collection_record(operate_time);
CREATE INDEX IF NOT EXISTS idx_record_method ON collection_record(method);
CREATE INDEX IF NOT EXISTS idx_record_target_type ON collection_record(target_type);
CREATE INDEX IF NOT EXISTS idx_record_actual_time ON collection_record(actual_collection_time);

-- 添加注释
COMMENT ON TABLE collection_record IS '催收记录表';
COMMENT ON COLUMN collection_record.method IS '催收方式: sms/phone/visit/litigation/mail/other';
COMMENT ON COLUMN collection_record.target_type IS '催收对象类型: borrower/guarantor/other';
COMMENT ON COLUMN collection_record.material_type IS '材料类型: image/audio/video/document';

-- ============================================
-- 3. 诉讼信息表 (litigation)
-- ============================================
CREATE TABLE IF NOT EXISTS litigation (
    litigation_id VARCHAR(32) PRIMARY KEY,
    loan_account VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    customer_name VARCHAR(100),
    status_code VARCHAR(20) NOT NULL,
    status_text VARCHAR(200),
    in_litigation BOOLEAN DEFAULT FALSE,
    -- 律所提交阶段
    submit_to_law_firm_date VARCHAR(20),
    submit_to_court_date VARCHAR(20),
    filing_case_no VARCHAR(100),
    -- 开庭判决阶段
    is_hearing BOOLEAN DEFAULT FALSE,
    hearing_date VARCHAR(20),
    judgment_date VARCHAR(20),
    -- 执行阶段
    execution_apply_to_court_date VARCHAR(20),
    execution_filing_date VARCHAR(20),
    execution_case_no VARCHAR(100),
    auction_status VARCHAR(50),
    -- 费用信息
    litigation_fee DECIMAL(18,2) DEFAULT 0,
    litigation_fee_paid_by_customer BOOLEAN DEFAULT FALSE,
    preservation_fee DECIMAL(18,2) DEFAULT 0,
    preservation_fee_paid_by_customer BOOLEAN DEFAULT FALSE,
    appraisal_fee DECIMAL(18,2) DEFAULT 0,
    litigation_preservation_paid_at VARCHAR(20),
    litigation_preservation_write_off_at VARCHAR(20),
    lawyer_fee DECIMAL(18,2) DEFAULT 0,
    lawyer_fee_paid_by_customer BOOLEAN DEFAULT FALSE,
    -- 机构信息
    court_name VARCHAR(200),
    law_firm VARCHAR(200),
    remark VARCHAR(500),
    extra_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_litigation_loan_account ON litigation(loan_account);
CREATE INDEX IF NOT EXISTS idx_litigation_customer_id ON litigation(customer_id);
CREATE INDEX IF NOT EXISTS idx_litigation_status_code ON litigation(status_code);
CREATE INDEX IF NOT EXISTS idx_litigation_updated_at ON litigation(updated_at);
CREATE INDEX IF NOT EXISTS idx_litigation_in_litigation ON litigation(in_litigation);

-- 添加注释
COMMENT ON TABLE litigation IS '诉讼信息表';
COMMENT ON COLUMN litigation.status_code IS '诉讼状态编码';
COMMENT ON COLUMN litigation.in_litigation IS '是否在诉';

-- ============================================
-- 4. 系统通知表 (notice)
-- ============================================
CREATE TABLE IF NOT EXISTS notice (
    notice_id VARCHAR(32) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    level VARCHAR(20),
    message VARCHAR(1000),
    customer_id VARCHAR(32),
    loan_account VARCHAR(32),
    customer_name VARCHAR(100),
    product_code VARCHAR(32),
    notice_type VARCHAR(50),
    overdue_days INT,
    is_read BOOLEAN DEFAULT FALSE,
    branch_code VARCHAR(20),
    extra_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_notice_customer_id ON notice(customer_id);
CREATE INDEX IF NOT EXISTS idx_notice_loan_account ON notice(loan_account);
CREATE INDEX IF NOT EXISTS idx_notice_is_read ON notice(is_read);
CREATE INDEX IF NOT EXISTS idx_notice_created_at ON notice(created_at);
CREATE INDEX IF NOT EXISTS idx_notice_branch_code ON notice(branch_code);

-- 添加注释
COMMENT ON TABLE notice IS '系统通知表';
COMMENT ON COLUMN notice.level IS '级别: high/medium/low';
COMMENT ON COLUMN notice.notice_type IS '通知类型，如 new_overdue/collecting_completed';
COMMENT ON COLUMN notice.branch_code IS '所属分支行号，用于业务员消息隔离';

-- ============================================
-- 5. 管辖行表 (jurisdiction_org)
-- ============================================
CREATE TABLE IF NOT EXISTS jurisdiction_org (
    id BIGSERIAL PRIMARY KEY,
    org_code VARCHAR(20) NOT NULL UNIQUE,
    org_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE jurisdiction_org IS '管辖行（上级机构）表';
COMMENT ON COLUMN jurisdiction_org.org_code IS '管辖行号';
COMMENT ON COLUMN jurisdiction_org.org_name IS '管辖行名称';

-- ============================================
-- 6. 分支行表 (branch_org)
-- ============================================
CREATE TABLE IF NOT EXISTS branch_org (
    id BIGSERIAL PRIMARY KEY,
    branch_code VARCHAR(20) NOT NULL,
    branch_name VARCHAR(100),
    org_code VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (branch_code, org_code)
);

CREATE INDEX IF NOT EXISTS idx_branch_org_org_code ON branch_org(org_code);

COMMENT ON TABLE branch_org IS '分支行（业务机构）表';
COMMENT ON COLUMN branch_org.branch_code IS '分支行号';
COMMENT ON COLUMN branch_org.branch_name IS '分支行名称';
COMMENT ON COLUMN branch_org.org_code IS '所属管辖行号';

-- ============================================
-- 7. 创建 updated_at 自动更新触发器函数
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为每个表创建触发器
DROP TRIGGER IF EXISTS update_loan_account_updated_at ON loan_account;
CREATE TRIGGER update_loan_account_updated_at
    BEFORE UPDATE ON loan_account
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

DROP TRIGGER IF EXISTS update_collection_record_updated_at ON collection_record;
CREATE TRIGGER update_collection_record_updated_at
    BEFORE UPDATE ON collection_record
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

DROP TRIGGER IF EXISTS update_litigation_updated_at ON litigation;
CREATE TRIGGER update_litigation_updated_at
    BEFORE UPDATE ON litigation
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

DROP TRIGGER IF EXISTS update_notice_updated_at ON notice;
CREATE TRIGGER update_notice_updated_at
    BEFORE UPDATE ON notice
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

DROP TRIGGER IF EXISTS update_jurisdiction_org_updated_at ON jurisdiction_org;
CREATE TRIGGER update_jurisdiction_org_updated_at
    BEFORE UPDATE ON jurisdiction_org
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

DROP TRIGGER IF EXISTS update_branch_org_updated_at ON branch_org;
CREATE TRIGGER update_branch_org_updated_at
    BEFORE UPDATE ON branch_org
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

-- ============================================
-- 测试数据（可选，生产环境请删除）
-- ============================================

-- 贷款账户测试数据
INSERT INTO loan_account (loan_account, customer_id, customer_name, branch_name, phone, product_code, product_name, loan_date, loan_term, overdue_days, contract_amount, loan_balance, unexpired_principal, overdue_principal, overdue_interest, overdue_penalty, total_overdue_amount, status) VALUES
('LA202501010001', '8800231', '张三', '广州市越秀支行', '13800138000', 'XFD001', '消费贷001', '2024-01-15', 12, 45, 100000.00, 85000.00, 70000.00, 15000.00, 450.00, 225.00, 15675.00, 'collecting'),
('LA202502020002', '8800233', '王五', '广州市越秀支行', '13800138001', 'XFY002', '消费贷002', '2024-02-20', 24, 30, 200000.00, 180000.00, 160000.00, 20000.00, 600.00, 300.00, 20900.00, 'uncollected'),
('LA202503030003', '8800234', '赵六', '广州市天河支行', '13900139001', 'XFD001', '消费贷001', '2024-03-10', 12, 15, 50000.00, 42000.00, 38000.00, 4000.00, 120.00, 60.00, 4180.00, 'completed')
ON CONFLICT (loan_account) DO NOTHING;

-- 催收记录测试数据
INSERT INTO collection_record (record_id, loan_account, customer_id, customer_name, method, method_text, result, operator_id, operator_name, operate_time, remark, material_type, material_name, material_url) VALUES
('R1001', 'LA202501010001', '8800231', '张三', 'sms', '短信', '已发送提醒短信', '954', '开发管理员', '2026-03-10 14:00:00', '模板：到期提醒', '', '', ''),
('R1002', 'LA202501010001', '8800231', '张三', 'phone', '电话', '客户承诺 3 日内处理', '1001', '业务员A', '2026-03-12 09:30:00', '客户反馈月底前还款', 'audio', 'call-20260312-0930.wav', 'https://example.com/files/call-20260312-0930.wav'),
('R1003', 'LA202502020002', '8800231', '张三', 'visit', '上门', '已上门核实客户经营情况', '1001', '业务员A', '2026-03-13 15:10:00', '已采集现场照片', 'image', 'visit-photo-20260313.jpg', 'https://example.com/files/visit-photo-20260313.jpg')
ON CONFLICT (record_id) DO NOTHING;

-- 诉讼信息测试数据
INSERT INTO litigation (litigation_id, loan_account, customer_id, customer_name, status_code, status_text, in_litigation, submit_to_law_firm_date, submit_to_court_date, filing_case_no, is_hearing, hearing_date, judgment_date, execution_apply_to_court_date, execution_filing_date, execution_case_no, auction_status, litigation_fee, litigation_fee_paid_by_customer, preservation_fee, preservation_fee_paid_by_customer, appraisal_fee, litigation_preservation_paid_at, litigation_preservation_write_off_at, lawyer_fee, lawyer_fee_paid_by_customer, court_name, law_firm, remark) VALUES
('L20260001', 'LA202501010001', '8800231', '张三', '2.2', '已立案待开庭', TRUE, '2026/3/10', '2026/3/12', '（2026）粤0104民初12345号', FALSE, '', '', '', '', '', '', 0.00, FALSE, 0.00, FALSE, 0.00, '', '', 1800.00, FALSE, '广州市越秀区人民法院', '广东正衡律师事务所', '已收到立案受理通知，待法院排期开庭'),
('L20250001', 'LA202501010001', '8800231', '张三', '3.7', '终结执行【注意2年内恢复执行，一般3个月内恢复执行】', FALSE, '2025/1/5', '2025/1/10', '（2025）粤0104民初5678号', TRUE, '2025/2/15', '2025/3/20', '2025/4/1', '2025/4/10', '（2025）粤0104执1234号', '流拍', 2500.00, FALSE, 800.00, FALSE, 500.00, '2025/4/5', '', 3000.00, FALSE, '广州市越秀区人民法院', '广东正衡律师事务所', '执行终结，待后续恢复执行')
ON CONFLICT (litigation_id) DO NOTHING;

-- 通知测试数据
INSERT INTO notice (notice_id, title, level, message, customer_id, loan_account, customer_name, product_code, notice_type, overdue_days, is_read, branch_code) VALUES
('N1001', '客户 8800231 贷款账户逾期提醒', 'high', '客户 8800231 的贷款账户 LA202501010001 已逾期 45 天，建议尽快完成电话提醒。', '8800231', 'LA202501010001', '张三', 'XFD001', 'new_overdue', 45, FALSE, NULL),
('N1002', '客户 8800232 贷款账户逾期提醒', 'medium', '客户 8800232 的贷款账户 LA202503030003 已逾期 15 天，请及时跟进。', '8800232', 'LA202503030003', '李四', 'XFD001', 'new_overdue', 15, FALSE, NULL),
('N1003', '新的催收任务分配', 'high', '您有新的催收账户需要处理，请及时查看。', '8800231', 'LA202502020002', '张三', 'XFY002', 'task_assign', 30, TRUE, NULL)
ON CONFLICT (notice_id) DO NOTHING;
