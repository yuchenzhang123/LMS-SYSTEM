-- ============================================
-- LMS催收管理系统 数据库完整初始化脚本
-- 数据库: GaussDB (PostgreSQL兼容)
-- 执行方式: 创建 lms schema 并在其中创建所有对象
-- ============================================

-- ============================================
-- 第一步: 创建 schema
-- ============================================

-- 删除已存在的 schema（会级联删除所有对象）
DROP SCHEMA IF EXISTS lms CASCADE;

-- 创建 schema
CREATE SCHEMA lms;

-- ============================================
-- 第二步: 创建表结构
-- ============================================

-- 1. 贷款账户表 (loan_account)
CREATE TABLE lms.loan_account (
    loan_account VARCHAR(32) PRIMARY KEY,
    customer_id VARCHAR(32) NOT NULL,
    customer_name VARCHAR(100),
    org_name VARCHAR(200),
    phone VARCHAR(20),
    product_code VARCHAR(32),
    product_name VARCHAR(100),
    loan_date DATE,
    loan_term INTEGER,
    overdue_days INTEGER DEFAULT 0,
    contract_amount DECIMAL(18,2),
    loan_balance DECIMAL(18,2),
    unexpired_principal DECIMAL(18,2),
    overdue_principal DECIMAL(18,2),
    overdue_interest DECIMAL(18,2),
    overdue_penalty DECIMAL(18,2),
    total_overdue_amount DECIMAL(18,2),
    status VARCHAR(20) DEFAULT 'uncollected',
    expected_days INTEGER DEFAULT 0,
    status_update_time TIMESTAMP,
    gbase_sync_time TIMESTAMP,
    gbase_raw_data TEXT,
    extra_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0
);

-- 2. 催收记录表 (collection_record)
CREATE TABLE lms.collection_record (
    record_id VARCHAR(32) PRIMARY KEY,
    loan_account VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    customer_name VARCHAR(100),
    target_type VARCHAR(20),
    target_name VARCHAR(100),
    actual_collection_time TIMESTAMP,
    method VARCHAR(20) NOT NULL,
    method_text VARCHAR(50),
    result VARCHAR(500),
    operator_id VARCHAR(32),
    operator_name VARCHAR(100),
    operate_time TIMESTAMP,
    remark VARCHAR(500),
    material_type VARCHAR(20),
    material_name VARCHAR(200),
    material_url VARCHAR(500),
    extra_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0
);

-- 3. 诉讼信息表 (litigation)
CREATE TABLE lms.litigation (
    litigation_id VARCHAR(32) PRIMARY KEY,
    loan_account VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    customer_name VARCHAR(100),
    status_code VARCHAR(20) NOT NULL,
    status_text VARCHAR(200),
    in_litigation BOOLEAN DEFAULT FALSE,
    submit_to_law_firm_date VARCHAR(20),
    submit_to_court_date VARCHAR(20),
    filing_case_no VARCHAR(100),
    is_hearing BOOLEAN DEFAULT FALSE,
    hearing_date VARCHAR(20),
    judgment_date VARCHAR(20),
    execution_apply_to_court_date VARCHAR(20),
    execution_filing_date VARCHAR(20),
    execution_case_no VARCHAR(100),
    auction_status VARCHAR(50),
    litigation_fee DECIMAL(18,2) DEFAULT 0,
    litigation_fee_paid_by_customer BOOLEAN DEFAULT FALSE,
    preservation_fee DECIMAL(18,2) DEFAULT 0,
    preservation_fee_paid_by_customer BOOLEAN DEFAULT FALSE,
    appraisal_fee DECIMAL(18,2) DEFAULT 0,
    litigation_preservation_paid_at VARCHAR(20),
    litigation_preservation_write_off_at VARCHAR(20),
    lawyer_fee DECIMAL(18,2) DEFAULT 0,
    lawyer_fee_paid_by_customer BOOLEAN DEFAULT FALSE,
    court_name VARCHAR(200),
    law_firm VARCHAR(200),
    remark VARCHAR(500),
    extra_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0
);

-- 4. 系统通知表 (notice)
CREATE TABLE lms.notice (
    notice_id VARCHAR(32) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    level VARCHAR(20),
    message VARCHAR(1000),
    customer_id VARCHAR(32),
    loan_account VARCHAR(32),
    customer_name VARCHAR(100),
    product_code VARCHAR(32),
    notice_type VARCHAR(50),
    overdue_days INTEGER,
    is_read BOOLEAN DEFAULT FALSE,
    extra_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0
);

-- ============================================
-- 第三步: 创建索引
-- ============================================

-- loan_account 索引
CREATE INDEX idx_loan_account_customer_id ON lms.loan_account(customer_id);
CREATE INDEX idx_loan_account_status ON lms.loan_account(status);
CREATE INDEX idx_loan_account_overdue_days ON lms.loan_account(overdue_days);
CREATE INDEX idx_loan_account_product_code ON lms.loan_account(product_code);

-- collection_record 索引
CREATE INDEX idx_record_loan_account ON lms.collection_record(loan_account);
CREATE INDEX idx_record_customer_id ON lms.collection_record(customer_id);
CREATE INDEX idx_record_operate_time ON lms.collection_record(operate_time);
CREATE INDEX idx_record_method ON lms.collection_record(method);
CREATE INDEX idx_record_target_type ON lms.collection_record(target_type);
CREATE INDEX idx_record_actual_time ON lms.collection_record(actual_collection_time);

-- litigation 索引
CREATE INDEX idx_litigation_loan_account ON lms.litigation(loan_account);
CREATE INDEX idx_litigation_customer_id ON lms.litigation(customer_id);
CREATE INDEX idx_litigation_status_code ON lms.litigation(status_code);
CREATE INDEX idx_litigation_updated_at ON lms.litigation(updated_at);
CREATE INDEX idx_litigation_in_litigation ON lms.litigation(in_litigation);

-- notice 索引
CREATE INDEX idx_notice_customer_id ON lms.notice(customer_id);
CREATE INDEX idx_notice_loan_account ON lms.notice(loan_account);
CREATE INDEX idx_notice_is_read ON lms.notice(is_read);
CREATE INDEX idx_notice_created_at ON lms.notice(created_at);

-- ============================================
-- 第四步: 添加表和字段注释
-- ============================================

-- loan_account 注释
COMMENT ON TABLE lms.loan_account IS '贷款账户表（从GBase转换存储）';
COMMENT ON COLUMN lms.loan_account.loan_account IS '贷款账号（业务主键）';
COMMENT ON COLUMN lms.loan_account.customer_id IS '客户ID';
COMMENT ON COLUMN lms.loan_account.customer_name IS '客户姓名';
COMMENT ON COLUMN lms.loan_account.org_name IS '机构名称';
COMMENT ON COLUMN lms.loan_account.phone IS '联系电话';
COMMENT ON COLUMN lms.loan_account.product_code IS '产品代码';
COMMENT ON COLUMN lms.loan_account.product_name IS '产品名称';
COMMENT ON COLUMN lms.loan_account.loan_date IS '放款日期';
COMMENT ON COLUMN lms.loan_account.loan_term IS '贷款期限';
COMMENT ON COLUMN lms.loan_account.overdue_days IS '逾期天数';
COMMENT ON COLUMN lms.loan_account.contract_amount IS '合同金额';
COMMENT ON COLUMN lms.loan_account.loan_balance IS '贷款余额';
COMMENT ON COLUMN lms.loan_account.unexpired_principal IS '未到期本金';
COMMENT ON COLUMN lms.loan_account.overdue_principal IS '逾期本金';
COMMENT ON COLUMN lms.loan_account.overdue_interest IS '逾期利息';
COMMENT ON COLUMN lms.loan_account.overdue_penalty IS '逾期罚息';
COMMENT ON COLUMN lms.loan_account.total_overdue_amount IS '逾期总金额';
COMMENT ON COLUMN lms.loan_account.status IS '状态: uncollected/collecting/completed';
COMMENT ON COLUMN lms.loan_account.expected_days IS '预计逾期天数';
COMMENT ON COLUMN lms.loan_account.status_update_time IS '状态更新时间';
COMMENT ON COLUMN lms.loan_account.gbase_sync_time IS 'GBase同步时间';
COMMENT ON COLUMN lms.loan_account.gbase_raw_data IS 'GBase原始数据';
COMMENT ON COLUMN lms.loan_account.extra_data IS '扩展数据';
COMMENT ON COLUMN lms.loan_account.created_at IS '创建时间';
COMMENT ON COLUMN lms.loan_account.updated_at IS '更新时间';
COMMENT ON COLUMN lms.loan_account.is_deleted IS '逻辑删除标记';

-- collection_record 注释
COMMENT ON TABLE lms.collection_record IS '催收记录表';
COMMENT ON COLUMN lms.collection_record.record_id IS '记录ID';
COMMENT ON COLUMN lms.collection_record.loan_account IS '贷款账号';
COMMENT ON COLUMN lms.collection_record.customer_id IS '客户ID';
COMMENT ON COLUMN lms.collection_record.customer_name IS '客户姓名';
COMMENT ON COLUMN lms.collection_record.target_type IS '催收对象类型: borrower/guarantor/other';
COMMENT ON COLUMN lms.collection_record.target_name IS '催收对象姓名';
COMMENT ON COLUMN lms.collection_record.actual_collection_time IS '实际催收时间';
COMMENT ON COLUMN lms.collection_record.method IS '催收方式: sms/phone/visit/litigation/mail/other';
COMMENT ON COLUMN lms.collection_record.method_text IS '催收方式文本';
COMMENT ON COLUMN lms.collection_record.result IS '催收结果';
COMMENT ON COLUMN lms.collection_record.operator_id IS '操作人ID';
COMMENT ON COLUMN lms.collection_record.operator_name IS '操作人姓名';
COMMENT ON COLUMN lms.collection_record.operate_time IS '操作时间';
COMMENT ON COLUMN lms.collection_record.remark IS '备注';
COMMENT ON COLUMN lms.collection_record.material_type IS '材料类型: image/audio/video/document';
COMMENT ON COLUMN lms.collection_record.material_name IS '材料名称';
COMMENT ON COLUMN lms.collection_record.material_url IS '材料URL';
COMMENT ON COLUMN lms.collection_record.extra_data IS '扩展数据';
COMMENT ON COLUMN lms.collection_record.created_at IS '创建时间';
COMMENT ON COLUMN lms.collection_record.updated_at IS '更新时间';
COMMENT ON COLUMN lms.collection_record.is_deleted IS '逻辑删除标记';

-- litigation 注释
COMMENT ON TABLE lms.litigation IS '诉讼信息表';
COMMENT ON COLUMN lms.litigation.litigation_id IS '诉讼ID';
COMMENT ON COLUMN lms.litigation.loan_account IS '贷款账号';
COMMENT ON COLUMN lms.litigation.customer_id IS '客户ID';
COMMENT ON COLUMN lms.litigation.customer_name IS '客户姓名';
COMMENT ON COLUMN lms.litigation.status_code IS '诉讼状态编码';
COMMENT ON COLUMN lms.litigation.status_text IS '诉讼状态文本';
COMMENT ON COLUMN lms.litigation.in_litigation IS '是否在诉';
COMMENT ON COLUMN lms.litigation.submit_to_law_firm_date IS '提交律所日期';
COMMENT ON COLUMN lms.litigation.submit_to_court_date IS '提交法院日期';
COMMENT ON COLUMN lms.litigation.filing_case_no IS '立案案号';
COMMENT ON COLUMN lms.litigation.is_hearing IS '是否已开庭';
COMMENT ON COLUMN lms.litigation.hearing_date IS '开庭日期';
COMMENT ON COLUMN lms.litigation.judgment_date IS '判决日期';
COMMENT ON COLUMN lms.litigation.execution_apply_to_court_date IS '申请执行日期';
COMMENT ON COLUMN lms.litigation.execution_filing_date IS '执行立案日期';
COMMENT ON COLUMN lms.litigation.execution_case_no IS '执行案号';
COMMENT ON COLUMN lms.litigation.auction_status IS '拍卖状态';
COMMENT ON COLUMN lms.litigation.litigation_fee IS '诉讼费';
COMMENT ON COLUMN lms.litigation.litigation_fee_paid_by_customer IS '诉讼费是否客户支付';
COMMENT ON COLUMN lms.litigation.preservation_fee IS '保全费';
COMMENT ON COLUMN lms.litigation.preservation_fee_paid_by_customer IS '保全费是否客户支付';
COMMENT ON COLUMN lms.litigation.appraisal_fee IS '评估费';
COMMENT ON COLUMN lms.litigation.litigation_preservation_paid_at IS '诉讼保全缴纳时间';
COMMENT ON COLUMN lms.litigation.litigation_preservation_write_off_at IS '诉讼保全核销时间';
COMMENT ON COLUMN lms.litigation.lawyer_fee IS '律师费';
COMMENT ON COLUMN lms.litigation.lawyer_fee_paid_by_customer IS '律师费是否客户支付';
COMMENT ON COLUMN lms.litigation.court_name IS '法院名称';
COMMENT ON COLUMN lms.litigation.law_firm IS '律所名称';
COMMENT ON COLUMN lms.litigation.remark IS '备注';
COMMENT ON COLUMN lms.litigation.extra_data IS '扩展数据';
COMMENT ON COLUMN lms.litigation.created_at IS '创建时间';
COMMENT ON COLUMN lms.litigation.updated_at IS '更新时间';
COMMENT ON COLUMN lms.litigation.is_deleted IS '逻辑删除标记';

-- notice 注释
COMMENT ON TABLE lms.notice IS '系统通知表';
COMMENT ON COLUMN lms.notice.notice_id IS '通知ID';
COMMENT ON COLUMN lms.notice.title IS '标题';
COMMENT ON COLUMN lms.notice.level IS '级别: high/medium/low';
COMMENT ON COLUMN lms.notice.message IS '消息内容';
COMMENT ON COLUMN lms.notice.customer_id IS '客户ID';
COMMENT ON COLUMN lms.notice.loan_account IS '贷款账号';
COMMENT ON COLUMN lms.notice.customer_name IS '客户姓名';
COMMENT ON COLUMN lms.notice.product_code IS '产品代码';
COMMENT ON COLUMN lms.notice.notice_type IS '通知类型';
COMMENT ON COLUMN lms.notice.overdue_days IS '逾期天数';
COMMENT ON COLUMN lms.notice.is_read IS '是否已读';
COMMENT ON COLUMN lms.notice.extra_data IS '扩展数据';
COMMENT ON COLUMN lms.notice.created_at IS '创建时间';
COMMENT ON COLUMN lms.notice.updated_at IS '更新时间';
COMMENT ON COLUMN lms.notice.is_deleted IS '逻辑删除标记';

-- ============================================
-- 第五步: 创建 updated_at 自动更新触发器
-- ============================================

-- 创建触发器函数
CREATE OR REPLACE FUNCTION lms.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器（使用 GaussDB 兼容的语法）
CREATE TRIGGER update_loan_account_updated_at
    BEFORE UPDATE ON lms.loan_account
    FOR EACH ROW
    EXECUTE PROCEDURE lms.update_updated_at_column();

CREATE TRIGGER update_collection_record_updated_at
    BEFORE UPDATE ON lms.collection_record
    FOR EACH ROW
    EXECUTE PROCEDURE lms.update_updated_at_column();

CREATE TRIGGER update_litigation_updated_at
    BEFORE UPDATE ON lms.litigation
    FOR EACH ROW
    EXECUTE PROCEDURE lms.update_updated_at_column();

CREATE TRIGGER update_notice_updated_at
    BEFORE UPDATE ON lms.notice
    FOR EACH ROW
    EXECUTE PROCEDURE lms.update_updated_at_column();

-- ============================================
-- 第六步: 插入测试数据（生产环境可删除此部分）
-- ============================================

-- 贷款账户测试数据
INSERT INTO lms.loan_account (loan_account, customer_id, customer_name, org_name, phone, product_code, product_name, loan_date, loan_term, overdue_days, contract_amount, loan_balance, unexpired_principal, overdue_principal, overdue_interest, overdue_penalty, total_overdue_amount, status) VALUES
('LA202501010001', '8800231', '张三', '广州市越秀支行', '13800138000', 'XFD001', '消费贷001', '2024-01-15', 12, 45, 100000.00, 85000.00, 70000.00, 15000.00, 450.00, 225.00, 15675.00, 'collecting'),
('LA202502020002', '8800233', '王五', '广州市越秀支行', '13800138001', 'XFY002', '消费贷002', '2024-02-20', 24, 30, 200000.00, 180000.00, 160000.00, 20000.00, 600.00, 300.00, 20900.00, 'uncollected'),
('LA202503030003', '8800234', '赵六', '广州市天河支行', '13900139001', 'XFD001', '消费贷001', '2024-03-10', 12, 15, 50000.00, 42000.00, 38000.00, 4000.00, 120.00, 60.00, 4180.00, 'completed');

-- 催收记录测试数据
INSERT INTO lms.collection_record (record_id, loan_account, customer_id, customer_name, method, method_text, result, operator_id, operator_name, operate_time, remark, material_type, material_name, material_url) VALUES
('R1001', 'LA202501010001', '8800231', '张三', 'sms', '短信', '已发送提醒短信', '954', '开发管理员', '2026-03-10 14:00:00', '模板：到期提醒', '', '', ''),
('R1002', 'LA202501010001', '8800231', '张三', 'phone', '电话', '客户承诺 3 日内处理', '1001', '业务员A', '2026-03-12 09:30:00', '客户反馈月底前还款', 'audio', 'call-20260312-0930.wav', 'https://example.com/files/call-20260312-0930.wav'),
('R1003', 'LA202502020002', '8800231', '张三', 'visit', '上门', '已上门核实客户经营情况', '1001', '业务员A', '2026-03-13 15:10:00', '已采集现场照片', 'image', 'visit-photo-20260313.jpg', 'https://example.com/files/visit-photo-20260313.jpg');

-- 诉讼信息测试数据
INSERT INTO lms.litigation (litigation_id, loan_account, customer_id, customer_name, status_code, status_text, in_litigation, submit_to_law_firm_date, submit_to_court_date, filing_case_no, is_hearing, hearing_date, judgment_date, execution_apply_to_court_date, execution_filing_date, execution_case_no, auction_status, litigation_fee, litigation_fee_paid_by_customer, preservation_fee, preservation_fee_paid_by_customer, appraisal_fee, litigation_preservation_paid_at, litigation_preservation_write_off_at, lawyer_fee, lawyer_fee_paid_by_customer, court_name, law_firm, remark) VALUES
('L20260001', 'LA202501010001', '8800231', '张三', '2.2', '已立案待开庭', TRUE, '2026/3/10', '2026/3/12', '（2026）粤0104民初12345号', FALSE, '', '', '', '', '', '', 0.00, FALSE, 0.00, FALSE, 0.00, '', '', 1800.00, FALSE, '广州市越秀区人民法院', '广东正衡律师事务所', '已收到立案受理通知，待法院排期开庭'),
('L20250001', 'LA202501010001', '8800231', '张三', '3.7', '终结执行【注意2年内恢复执行，一般3个月内恢复执行】', FALSE, '2025/1/5', '2025/1/10', '（2025）粤0104民初5678号', TRUE, '2025/2/15', '2025/3/20', '2025/4/1', '2025/4/10', '（2025）粤0104执1234号', '流拍', 2500.00, FALSE, 800.00, FALSE, 500.00, '2025/4/5', '', 3000.00, FALSE, '广州市越秀区人民法院', '广东正衡律师事务所', '执行终结，待后续恢复执行');

-- 通知测试数据
INSERT INTO lms.notice (notice_id, title, level, message, customer_id, loan_account, customer_name, product_code, notice_type, overdue_days, is_read) VALUES
('N1001', '客户 8800231 贷款账户逾期提醒', 'high', '客户 8800231 的贷款账户 LA202501010001 已逾期 45 天，建议尽快完成电话提醒。', '8800231', 'LA202501010001', '张三', 'XFD001', 'new_overdue', 45, FALSE),
('N1002', '客户 8800232 贷款账户逾期提醒', 'medium', '客户 8800232 的贷款账户 LA202503030003 已逾期 15 天，请及时跟进。', '8800232', 'LA202503030003', '李四', 'XFD001', 'new_overdue', 15, FALSE),
('N1003', '新的催收任务分配', 'high', '您有新的催收账户需要处理，请及时查看。', '8800231', 'LA202502020002', '张三', 'XFY002', 'task_assign', 30, TRUE);

-- ============================================
-- 第七步: 执行完成提示
-- ============================================

-- 显示创建结果
SELECT 'LMS Schema 初始化完成！' AS message;

SELECT 
    'loan_account' AS table_name, COUNT(*) AS record_count FROM lms.loan_account
UNION ALL
SELECT 'collection_record', COUNT(*) FROM lms.collection_record
UNION ALL
SELECT 'litigation', COUNT(*) FROM lms.litigation
UNION ALL
SELECT 'notice', COUNT(*) FROM lms.notice;
