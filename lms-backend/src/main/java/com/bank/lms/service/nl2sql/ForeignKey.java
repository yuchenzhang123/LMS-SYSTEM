package com.bank.lms.service.nl2sql;

import lombok.Data;

/**
 * 表间外键关系（Join Graph 的一条边）。
 *
 * 用途：两步式 Schema Linking 的第一步「选表」与第二步「生成 SQL」都会把外键图喂给 LLM，
 * 让 LLM 知道多表如何关联（如 collection_record.operator_id = user_org.ehr_no），
 * 从而减少 JOIN 写错（DIN-SQL / RAT-SQL 的「外键喂给模型」思路）。
 *
 * 注意：此结构仅用于「告知 LLM 关联关系」，权限安全仍由 SqlSafetyGuard 逐表强制注入兜底，
 * 不依赖此处声明是否完整。
 */
@Data
public class ForeignKey {
    /** 本表名，如 collection_record */
    private String fromTable;
    /** 本表关联列，如 operator_id */
    private String fromColumn;
    /** 目标表名，如 user_org */
    private String toTable;
    /** 目标表关联列，如 ehr_no */
    private String toColumn;
}
