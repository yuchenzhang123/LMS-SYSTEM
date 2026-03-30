CREATE INDEX idx_loan_account_customer_id ON lms.loan_account(customer_id);
CREATE INDEX idx_loan_account_status ON lms.loan_account(status);
CREATE INDEX idx_loan_account_overdue_days ON lms.loan_account(overdue_days);
CREATE INDEX idx_loan_account_product_code ON lms.loan_account(product_code);

CREATE INDEX idx_record_loan_account ON lms.collection_record(loan_account);
CREATE INDEX idx_record_customer_id ON lms.collection_record(customer_id);
CREATE INDEX idx_record_operate_time ON lms.collection_record(operate_time);
CREATE INDEX idx_record_method ON lms.collection_record(method);
CREATE INDEX idx_record_target_type ON lms.collection_record(target_type);
CREATE INDEX idx_record_actual_time ON lms.collection_record(actual_collection_time);

CREATE INDEX idx_litigation_loan_account ON lms.litigation(loan_account);
CREATE INDEX idx_litigation_customer_id ON lms.litigation(customer_id);
CREATE INDEX idx_litigation_status_code ON lms.litigation(status_code);
CREATE INDEX idx_litigation_updated_at ON lms.litigation(updated_at);
CREATE INDEX idx_litigation_in_litigation ON lms.litigation(in_litigation);

CREATE INDEX idx_notice_customer_id ON lms.notice(customer_id);
CREATE INDEX idx_notice_loan_account ON lms.notice(loan_account);
CREATE INDEX idx_notice_is_read ON lms.notice(is_read);
CREATE INDEX idx_notice_created_at ON lms.notice(created_at);
