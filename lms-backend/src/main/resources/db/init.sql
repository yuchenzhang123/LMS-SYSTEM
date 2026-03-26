-- ============================================
-- LMS催收管理系统 数据库初始化脚本
-- 数据库: lms_db (GaussDB/MySQL)
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS lms_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE lms_db;

-- ============================================
-- 1. 贷款账户表 (loan_account)
-- ============================================
CREATE TABLE IF NOT EXISTS loan_account (
    loan_account VARCHAR(32) PRIMARY KEY COMMENT '贷款账号（业务主键）',
    customer_id VARCHAR(32) NOT NULL COMMENT '客户ID',
    customer_name VARCHAR(100) COMMENT '客户姓名（冗余，方便查询）',
    org_name VARCHAR(200) COMMENT '所属机构',
    phone VARCHAR(20) COMMENT '联系电话',
    product_code VARCHAR(32) COMMENT '产品代码',
    product_name VARCHAR(100) COMMENT '产品名称（冗余）',
    loan_date DATE COMMENT '放款日期',
    loan_term INT COMMENT '贷款期限(月)',
    overdue_days INT DEFAULT 0 COMMENT '逾期天数',
    overdue_times INT DEFAULT 0 COMMENT '逾期次数',
    contract_amount DECIMAL(18,2) COMMENT '合同金额',
    loan_balance DECIMAL(18,2) COMMENT '贷款余额',
    unexpired_principal DECIMAL(18,2) COMMENT '未到期本金',
    overdue_principal DECIMAL(18,2) COMMENT '逾期本金',
    overdue_interest DECIMAL(18,2) COMMENT '逾期利息',
    overdue_penalty DECIMAL(18,2) COMMENT '逾期罚息',
    total_overdue_amount DECIMAL(18,2) COMMENT '逾期总额',
    status VARCHAR(20) DEFAULT 'uncollected' COMMENT '状态: uncollected/collecting/completed',
    expected_days INT DEFAULT 0 COMMENT '预期天数',
    status_update_time TIMESTAMP NULL COMMENT '状态更新时间',
    gbase_sync_time TIMESTAMP NULL COMMENT 'GBase同步时间',
    gbase_raw_data TEXT COMMENT 'GBase原始数据（JSON格式，便于后续扩展）',
    extra_data TEXT COMMENT '扩展字段（JSON格式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status),
    INDEX idx_overdue_days (overdue_days),
    INDEX idx_product_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款账户表（从GBase转换存储）';

-- ============================================
-- 2. 催收记录表 (collection_record)
-- ============================================
CREATE TABLE IF NOT EXISTS collection_record (
    record_id VARCHAR(32) PRIMARY KEY COMMENT '记录ID（业务主键）',
    loan_account VARCHAR(32) NOT NULL COMMENT '贷款账号',
    customer_id VARCHAR(32) NOT NULL COMMENT '客户ID',
    customer_name VARCHAR(100) COMMENT '客户姓名（冗余）',
    target_type VARCHAR(20) COMMENT '催收对象类型: borrower/guarantor/other',
    target_name VARCHAR(100) COMMENT '催收对象姓名',
    actual_collection_time TIMESTAMP NULL COMMENT '实际催收时间',
    method VARCHAR(20) NOT NULL COMMENT '催收方式: sms/phone/visit/litigation/mail/other',
    method_text VARCHAR(50) COMMENT '催收方式文本',
    result VARCHAR(500) COMMENT '催收结果',
    operator_id VARCHAR(32) COMMENT '操作员ID',
    operator_name VARCHAR(100) COMMENT '操作员姓名',
    operate_time TIMESTAMP NULL COMMENT '操作时间',
    remark VARCHAR(500) COMMENT '备注',
    material_type VARCHAR(20) COMMENT '材料类型: image/audio/video/document',
    material_name VARCHAR(200) COMMENT '材料名称',
    material_url VARCHAR(500) COMMENT '材料URL',
    extra_data TEXT COMMENT '扩展字段（JSON格式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_loan_account (loan_account),
    INDEX idx_customer_id (customer_id),
    INDEX idx_operate_time (operate_time),
    INDEX idx_method (method),
    INDEX idx_target_type (target_type),
    INDEX idx_actual_collection_time (actual_collection_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='催收记录表';

-- ============================================
-- 3. 诉讼信息表 (litigation)
-- ============================================
CREATE TABLE IF NOT EXISTS litigation (
    litigation_id VARCHAR(32) PRIMARY KEY COMMENT '诉讼ID（业务主键）',
    loan_account VARCHAR(32) NOT NULL COMMENT '贷款账号',
    customer_id VARCHAR(32) NOT NULL COMMENT '客户ID',
    customer_name VARCHAR(100) COMMENT '客户姓名（冗余）',
    status_code VARCHAR(20) NOT NULL COMMENT '诉讼状态编码',
    status_text VARCHAR(200) COMMENT '诉讼状态文本',
    in_litigation BOOLEAN DEFAULT FALSE COMMENT '是否在诉',
    -- 律所提交阶段
    submit_to_law_firm_date VARCHAR(20) COMMENT '提交律所日期',
    submit_to_court_date VARCHAR(20) COMMENT '提交法院日期',
    filing_case_no VARCHAR(100) COMMENT '立案案号',
    -- 开庭判决阶段
    is_hearing BOOLEAN DEFAULT FALSE COMMENT '是否已开庭',
    hearing_date VARCHAR(20) COMMENT '开庭日期',
    judgment_date VARCHAR(20) COMMENT '判决日期',
    -- 执行阶段
    execution_apply_to_court_date VARCHAR(20) COMMENT '申请执行日期',
    execution_filing_date VARCHAR(20) COMMENT '执行立案日期',
    execution_case_no VARCHAR(100) COMMENT '执行案号',
    auction_status VARCHAR(50) COMMENT '拍卖状态',
    -- 费用信息
    litigation_fee DECIMAL(18,2) DEFAULT 0 COMMENT '诉讼费',
    litigation_fee_paid_by_customer BOOLEAN DEFAULT FALSE COMMENT '诉讼费客户承担',
    preservation_fee DECIMAL(18,2) DEFAULT 0 COMMENT '保全费',
    preservation_fee_paid_by_customer BOOLEAN DEFAULT FALSE COMMENT '保全费客户承担',
    appraisal_fee DECIMAL(18,2) DEFAULT 0 COMMENT '评估费',
    litigation_preservation_paid_at VARCHAR(20) COMMENT '诉讼保全缴纳时间',
    litigation_preservation_write_off_at VARCHAR(20) COMMENT '诉讼保全核销时间',
    lawyer_fee DECIMAL(18,2) DEFAULT 0 COMMENT '律师费',
    lawyer_fee_paid_by_customer BOOLEAN DEFAULT FALSE COMMENT '律师费客户承担',
    -- 机构信息
    court_name VARCHAR(200) COMMENT '法院名称',
    law_firm VARCHAR(200) COMMENT '律所名称',
    remark VARCHAR(500) COMMENT '备注',
    extra_data TEXT COMMENT '扩展字段（JSON格式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_loan_account (loan_account),
    INDEX idx_customer_id (customer_id),
    INDEX idx_status_code (status_code),
    INDEX idx_updated_at (updated_at),
    INDEX idx_in_litigation (in_litigation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诉讼信息表';

-- ============================================
-- 4. 系统通知表 (notice)
-- ============================================
CREATE TABLE IF NOT EXISTS notice (
    notice_id VARCHAR(32) PRIMARY KEY COMMENT '通知ID（业务主键）',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    level VARCHAR(20) COMMENT '级别: high/medium/low',
    message VARCHAR(1000) COMMENT '消息内容',
    customer_id VARCHAR(32) COMMENT '客户ID',
    loan_account VARCHAR(32) COMMENT '贷款账号',
    customer_name VARCHAR(100) COMMENT '客户姓名（冗余）',
    product_code VARCHAR(32) COMMENT '产品代码',
    notice_type VARCHAR(50) COMMENT '通知类型，如 new_overdue/collecting_completed',
    overdue_days INT COMMENT '逾期天数',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    extra_data TEXT COMMENT '扩展字段（JSON格式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_customer_id (customer_id),
    INDEX idx_loan_account (loan_account),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';

-- ============================================
-- 初始化测试数据
-- ============================================

-- 贷款账户测试数据
INSERT INTO loan_account (loan_account, customer_id, customer_name, org_name, phone, product_code, product_name, loan_date, loan_term, overdue_days, overdue_times, contract_amount, loan_balance, unexpired_principal, overdue_principal, overdue_interest, overdue_penalty, total_overdue_amount, status) VALUES
('LA202501010001', '8800231', '张三', '广州市越秀支行', '13800138000', 'XFD001', '消费贷001', '2024-01-15', 12, 45, 2, 100000.00, 85000.00, 70000.00, 15000.00, 450.00, 225.00, 15675.00, 'collecting'),
('LA202502020002', '8800233', '王五', '广州市越秀支行', '13800138001', 'XFY002', '消费贷002', '2024-02-20', 24, 30, 1, 200000.00, 180000.00, 160000.00, 20000.00, 600.00, 300.00, 20900.00, 'uncollected'),
('LA202503030003', '8800234', '赵六', '广州市天河支行', '13900139001', 'XFD001', '消费贷001', '2024-03-10', 12, 15, 3, 50000.00, 42000.00, 38000.00, 4000.00, 120.00, 60.00, 4180.00, 'completed');

-- 催收记录测试数据
INSERT INTO collection_record (record_id, loan_account, customer_id, customer_name, method, method_text, result, operator_id, operator_name, operate_time, remark, material_type, material_name, material_url) VALUES
('R1001', 'LA202501010001', '8800231', '张三', 'sms', '短信', '已发送提醒短信', '954', '开发管理员', '2026-03-10 14:00:00', '模板：到期提醒', '', '', ''),
('R1002', 'LA202501010001', '8800231', '张三', 'phone', '电话', '客户承诺 3 日内处理', '1001', '业务员A', '2026-03-12 09:30:00', '客户反馈月底前还款', 'audio', 'call-20260312-0930.wav', 'https://example.com/files/call-20260312-0930.wav'),
('R1003', 'LA202502020002', '8800231', '张三', 'visit', '上门', '已上门核实客户经营情况', '1001', '业务员A', '2026-03-13 15:10:00', '已采集现场照片', 'image', 'visit-photo-20260313.jpg', 'https://example.com/files/visit-photo-20260313.jpg');

-- 诉讼信息测试数据
INSERT INTO litigation (litigation_id, loan_account, customer_id, customer_name, status_code, status_text, in_litigation, submit_to_law_firm_date, submit_to_court_date, filing_case_no, is_hearing, hearing_date, judgment_date, execution_apply_to_court_date, execution_filing_date, execution_case_no, auction_status, litigation_fee, litigation_fee_paid_by_customer, preservation_fee, preservation_fee_paid_by_customer, appraisal_fee, litigation_preservation_paid_at, litigation_preservation_write_off_at, lawyer_fee, lawyer_fee_paid_by_customer, court_name, law_firm, remark) VALUES
('L20260001', 'LA202501010001', '8800231', '张三', '2.2', '已立案待开庭', TRUE, '2026/3/10', '2026/3/12', '（2026）粤0104民初12345号', FALSE, '', '', '', '', '', '', 0.00, FALSE, 0.00, FALSE, 0.00, '', '', 1800.00, FALSE, '广州市越秀区人民法院', '广东正衡律师事务所', '已收到立案受理通知，待法院排期开庭'),
('L20250001', 'LA202501010001', '8800231', '张三', '3.7', '终结执行【注意2年内恢复执行，一般3个月内恢复执行】', FALSE, '2025/1/5', '2025/1/10', '（2025）粤0104民初5678号', TRUE, '2025/2/15', '2025/3/20', '2025/4/1', '2025/4/10', '（2025）粤0104执1234号', '流拍', 2500.00, FALSE, 800.00, FALSE, 500.00, '2025/4/5', '', 3000.00, FALSE, '广州市越秀区人民法院', '广东正衡律师事务所', '执行终结，待后续恢复执行');

-- 通知测试数据
INSERT INTO notice (notice_id, title, level, message, customer_id, loan_account, customer_name, product_code, notice_type, overdue_days, is_read) VALUES
('N1001', '客户 8800231 贷款账户逾期提醒', 'high', '客户 8800231 的贷款账户 LA202501010001 已逾期 45 天，建议尽快完成电话提醒。', '8800231', 'LA202501010001', '张三', 'XFD001', 'new_overdue', 45, FALSE),
('N1002', '客户 8800232 贷款账户逾期提醒', 'medium', '客户 8800232 的贷款账户 LA202503030003 已逾期 15 天，请及时跟进。', '8800232', 'LA202503030003', '李四', 'XFD001', 'new_overdue', 15, FALSE),
('N1003', '新的催收任务分配', 'high', '您有新的催收账户需要处理，请及时查看。', '8800231', 'LA202502020002', '张三', 'XFY002', 'task_assign', 30, TRUE);
