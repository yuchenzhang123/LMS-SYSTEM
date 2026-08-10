package com.bank.lms.service.analysis;

import java.util.Arrays;
import java.util.List;

/**
 * 预定义分析能力枚举
 * 每种能力绑定预编译 JPQL，LLM 不生成 SQL，零越权风险
 */
public enum AnalysisCapability {

    OVERDUE_TREND("逾期趋势", "近N天每日新增逾期笔数和金额"),
    OVERDUE_AGING("逾期账龄", "各账龄段（1-7/8-30/31-60/60+天）的账户数分布及占比变化"),
    COMPLETION_RATE("催收完成率", "近N天每日催收完成率趋势"),
    ORG_RANKING("机构排名", "组内机构按逾期数/逾期额/完成率的多维度排名"),
    HIGH_OVERDUE_BACKLOG("深度逾期积压", "逾期超过30天未结清的账户积压趋势"),
    YOY_COMPARISON("同比分析", "本月 vs 去年同月的逾期数据对比"),
    EMPLOYEE_WORKLOAD("员工工作量", "员工催收次数排名，与组内均值对比"),
    EMPLOYEE_EFFICIENCY("员工效率", "员工催收完成率连续变化趋势"),
    TEAM_CAPACITY("团队产能", "组内活跃催收人数、人均逾期账户数"),
    LOGIN_ACTIVITY("登录活跃度", "员工连续未登录天数、组内登录率"),
    COLLECTION_RESPONSE("催收响应时效", "新增逾期到首次催收的平均间隔"),
    DAILY_BRIEFING("每日简报", "Dashboard/洞察页的每日业务概述"),
    COLLECTION_SUMMARY("催收历程摘要", "单账户所有催收记录的总结");

    private final String displayName;
    private final String description;

    AnalysisCapability(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    /**
     * 构建 LLM 意图识别用的能力列表描述
     */
    public static String buildCapabilityDescriptionForLlm() {
        StringBuilder sb = new StringBuilder();
        for (AnalysisCapability cap : values()) {
            sb.append("- ").append(cap.name())
              .append(": ").append(cap.getDescription()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 根据名称查找能力
     */
    public static AnalysisCapability findByName(String name) {
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }
}
