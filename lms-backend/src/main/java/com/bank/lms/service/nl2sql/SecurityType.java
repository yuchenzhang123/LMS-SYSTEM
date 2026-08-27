package com.bank.lms.service.nl2sql;

/**
 * 表的安全类型，决定 SqlSafetyGuard 如何注入行级权限过滤。
 * 这是 NL2SQL 零越权的核心分类，映射必须与 AiQueryInterceptor 的权限模型一致。
 */
public enum SecurityType {
    /** 直接含 branch_code 列，注入 branch_code IN (:branchCodes) */
    DIRECT_BRANCH,
    /** 直接含 org_code 列，注入 org_code IN (:orgCodes) */
    DIRECT_ORG,
    /** 同时含 branch_code 和 org_code，两者都注入 */
    DIRECT_BOTH,
    /** 无机构字段，必须 JOIN loan_account 后用其 branch_code 过滤 */
    VIA_JOIN,
    /** 无需行级过滤（暂未使用，预留） */
    NONE
}
