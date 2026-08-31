package com.bank.lms.service.nl2sql;

import lombok.Data;

import java.util.List;

/**
 * 两步式 Schema Linking 第一步「选表」的 LLM 输出。
 * LLM 先判断意图并挑出与问题相关的表，第二步再用这些表的精简 schema 生成 SQL。
 */
@Data
public class TableSelection {
    /** intent 取值：nl2sql（需要查库）| chat（闲聊，不查库） */
    private String intent;
    /** intent=nl2sql 时选中的表名集合（来自 SchemaRegistry 白名单），可空（空=降级全量） */
    private List<String> tables;
}
