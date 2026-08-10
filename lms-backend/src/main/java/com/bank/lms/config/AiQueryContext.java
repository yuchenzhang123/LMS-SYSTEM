package com.bank.lms.config;

import com.bank.lms.dto.analysis.AiUserScope;

/**
 * AI 查询上下文（ThreadLocal），存储当前请求的用户范围
 * 确保 AI 分析中的数据权限隔离
 */
public class AiQueryContext {

    private static final ThreadLocal<AiUserScope> SCOPE = new ThreadLocal<>();

    public static void set(AiUserScope scope) {
        SCOPE.set(scope);
    }

    public static AiUserScope get() {
        return SCOPE.get();
    }

    public static void clear() {
        SCOPE.remove();
    }
}
