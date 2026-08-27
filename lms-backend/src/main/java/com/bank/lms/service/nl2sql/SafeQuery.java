package com.bank.lms.service.nl2sql;

import lombok.Data;

import java.util.Map;

/**
 * 安全校验 + 行级注入后的可执行查询。
 */
@Data
public class SafeQuery {
    /** 重写后的 SQL（已注入行级过滤 + 软删过滤 + LIMIT） */
    private String rewrittenSql;
    /** 命名参数（如 branchCodes/orgCodes → List），供 NamedParameterJdbcTemplate 展开 */
    private Map<String, Object> params;
}
