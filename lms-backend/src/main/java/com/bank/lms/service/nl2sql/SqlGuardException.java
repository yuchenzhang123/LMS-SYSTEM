package com.bank.lms.service.nl2sql;

/**
 * SQL 安全校验失败异常。message 面向 LLM 修正轮，需写清楚被拒绝的原因。
 */
public class SqlGuardException extends RuntimeException {

    public SqlGuardException(String message) {
        super(message);
    }
}
