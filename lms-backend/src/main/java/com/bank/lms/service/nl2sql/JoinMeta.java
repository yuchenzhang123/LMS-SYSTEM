package com.bank.lms.service.nl2sql;

import lombok.Data;

/**
 * VIA_JOIN 表到 loan_account 的关联关系。
 * 例如 collection_record.loan_account = loan_account.loan_account
 */
@Data
public class JoinMeta {
    /** 本表名，如 collection_record */
    private String tableName;
    /** 本表关联列，如 collection_record.loan_account */
    private String thisColumn;
    /** 目标表名，恒为 loan_account */
    private String targetTable;
    /** 目标表关联列，恒为 loan_account.loan_account */
    private String targetColumn;
}
