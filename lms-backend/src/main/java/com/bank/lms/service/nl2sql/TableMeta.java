package com.bank.lms.service.nl2sql;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 表元数据（schema 召回字典的一表）。
 */
@Data
public class TableMeta {
    /** 数据库表名（snake_case，如 loan_account） */
    private String name;
    /** 中文表释义（如「贷款账户表」） */
    private String comment;
    /** 列字典，LinkedHashMap 保序，key 为列名 */
    private Map<String, ColumnMeta> columns;
    /** 安全类型，决定行级过滤注入方式 */
    private SecurityType security;
    /** 仅 VIA_JOIN 类型有值，描述到 loan_account 的关联（权限代理用） */
    private JoinMeta joinToLoanAccount;
    /** 本表作为 from 的外键边集合（喂 LLM 用，非权限依据），无外键时为空 */
    private List<ForeignKey> foreignKeys;
    /** 是否有 is_deleted 软删字段（继承 BaseEntity 的表），JDBC 查询时守卫需自动注入 is_deleted=0 */
    private boolean softDeleted;
}
