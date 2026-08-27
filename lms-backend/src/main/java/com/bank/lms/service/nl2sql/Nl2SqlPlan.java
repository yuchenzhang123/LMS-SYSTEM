package com.bank.lms.service.nl2sql;

import lombok.Data;

import java.util.Map;

/**
 * LLM 规划输出的 JSON（Step1 解析结果）。
 */
@Data
public class Nl2SqlPlan {
    /** intent 取值：nl2sql（自由查询）| chat（闲聊，不查库） */
    private String intent;
    /** intent=nl2sql 时由 LLM 生成的 SELECT */
    private String sql;
    /** 预留参数（如时间范围），当前未使用 */
    private Map<String, Object> params;
    /** 意图不明确时的澄清话术（可选） */
    private String clarification;
}
