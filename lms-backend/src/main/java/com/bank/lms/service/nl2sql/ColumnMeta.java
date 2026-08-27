package com.bank.lms.service.nl2sql;

import lombok.Data;

/**
 * 列元数据（schema 召回字典的一列）。
 * 实体 JPA 无中文字段释义注解，中文含义在此手工维护。
 */
@Data
public class ColumnMeta {
    /** 数据库列名（snake_case，如 branch_code） */
    private String name;
    /** 中文含义（供 LLM 理解，如「分支行号」） */
    private String comment;
    /** JDBC 类型标签（VARCHAR/NUMERIC/DECIMAL/INT/DATE/DATETIME/TEXT 等），供 prompt 展示 */
    private String jdbcType;
    /** 是否为 branch_code 行级权限列 */
    private boolean branchCode;
    /** 是否为 org_code 行级权限列 */
    private boolean orgCode;
    /** 是否为敏感列（手机号/身份证等），默认禁止查询 */
    private boolean pii;
}
