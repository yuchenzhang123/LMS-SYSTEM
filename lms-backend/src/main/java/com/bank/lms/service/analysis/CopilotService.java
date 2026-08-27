package com.bank.lms.service.analysis;

import com.bank.lms.config.AiQueryContext;
import com.bank.lms.dto.analysis.AiUserScope;
import com.bank.lms.entity.CollectionRecord;
import com.bank.lms.entity.LoanAccount;
import com.bank.lms.repository.CollectionRecordRepository;
import com.bank.lms.repository.LoanAccountRepository;
import com.bank.lms.service.LoanAccountService;
import com.bank.lms.service.llm.LlmClient;
import com.bank.lms.service.nl2sql.Nl2SqlPlan;
import com.bank.lms.service.nl2sql.Nl2SqlResult;
import com.bank.lms.service.nl2sql.Nl2SqlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 助手服务
 * 提供：AI 问答、每日简报、催收历程摘要
 * LLM 只负责"理解意图"和"文字表达"，SQL 由 Nl2SqlService（NL2SQL）执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopilotService {

    private final LlmClient llmClient;
    private final CollectionRecordRepository collectionRecordRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final LoanAccountService loanAccountService;
    private final Nl2SqlService nl2SqlService;

    // ==================== 各功能思考模式开关（可在 yml 按功能配置） ====================

    /** AI问答（归因分析）：需要多步推理，默认开 */
    @Value("${lms.llm.thinking.ask:true}")
    private boolean thinkingAsk;

    /** 每日简报：数据直出，默认关 */
    @Value("${lms.llm.thinking.briefing:false}")
    private boolean thinkingBriefing;

    /** 催收摘要：归纳直出，默认关 */
    @Value("${lms.llm.thinking.summary:false}")
    private boolean thinkingSummary;

    private static final String SYSTEM_PROMPT =
        "你是一个银行贷后催收管理系统的数据分析助手。你的职责是：\n" +
        "1. 理解用户用自然语言提出的业务问题\n" +
        "2. 将数据转化为简洁易懂的中文分析，每次回答不超过150字\n" +
        "3. 不要编造数据，所有数据必须来自给出的分析结果\n";

    /** 直接对话兜底文案：无法理解用户问题 */
    private static final String FALLBACK_UNKNOWN =
        "抱歉，我暂时无法理解您的问题。您可以尝试问：逾期趋势、机构排名、员工工作量、催收完成率等。";

    /** 直接对话兜底文案：问候/闲聊 */
    private static final String FALLBACK_GREETING =
        "您好，我是贷后催收数据分析助手，可以帮您查询逾期趋势、机构排名、员工工作量、催收完成率等数据。";

    /**
     * AI 问答（JSON 两步法规划路由）
     *
     * Step1：LLM 输出规划 JSON {intent, sql}
     * Step2：Java 按 intent 路由——
     *   nl2sql → 自由查询（Nl2SqlService 守卫 + 执行 + 修正 + 润色）
     *   chat   → 不查库，直接文字回答
     * 规划失败/LLM 不可用时降级为直接对话（不查库）。
     */
    public Map<String, Object> ask(String question) {
        AiUserScope scope = AiQueryContext.get();
        Map<String, Object> result = new HashMap<>();
        result.put("question", question);

        // Step1：LLM 规划
        Nl2SqlPlan plan = nl2SqlService.plan(question);
        if (plan == null || plan.getIntent() == null || plan.getIntent().trim().isEmpty()) {
            // LLM 不可用或规划失败 → 降级为直接对话
            return directChat(question, result, FALLBACK_UNKNOWN);
        }

        String intent = plan.getIntent().trim().toLowerCase();
        log.debug("AI问答规划路由: question={}, intent={}", question, intent);

        // Step2：按 intent 路由
        switch (intent) {
            case "nl2sql": {
                if (plan.getSql() == null || plan.getSql().trim().isEmpty()) {
                    return directChat(question, result, FALLBACK_UNKNOWN);
                }
                Nl2SqlResult r = nl2SqlService.executeAndAnswer(plan.getSql(), question, scope);
                result.put("answer", r.getAnswerText());
                result.put("capability", "nl2sql");
                result.put("intent", "nl2sql");
                result.put("data", r.getRows());
                result.put("columns", r.getColumns());
                return result;
            }
            case "chat":
            default:
                return directChat(question, result, FALLBACK_GREETING);
        }
    }

    /**
     * 直接对话兜底（不查库）。规划失败 / chat 意图 / 未知意图统一走这里。
     */
    private Map<String, Object> directChat(String question, Map<String, Object> result, String fallbackText) {
        String answer = llmEnabled()
            ? llmClient.chat(SYSTEM_PROMPT, question, thinkingAsk)
            : null;
        result.put("answer", answer != null ? answer : fallbackText);
        result.put("capability", "chat");
        return result;
    }

    // ==================== 简报缓存（按用户数据范围隔离；主失效靠同步后清空，TTL 仅兜底） ====================

    /** 简报缓存兜底 TTL（分钟）。主失效机制是数据同步完成后 clearBriefingCache()，此 TTL 仅防同步任务故障导致缓存无限期 */
    @Value("${lms.llm.cache.briefing-ttl-minutes:1500}")
    private int briefingTtlMinutes;

    private final java.util.concurrent.ConcurrentHashMap<String, BriefingCacheEntry> briefingCache =
        new java.util.concurrent.ConcurrentHashMap<>();

    private static class BriefingCacheEntry {
        final Map<String, Object> result;
        final long createdAt;
        BriefingCacheEntry(Map<String, Object> result) {
            this.result = result;
            this.createdAt = System.currentTimeMillis();
        }
    }

    /**
     * 每日简报（带缓存）
     * 缓存 key = 用户数据范围（allowedBranchCodes 排序拼接），保证不同机构/不同权限各看各的
     * 主失效：数据同步完成后 clearBriefingCache()；兜底：TTL 超时（默认 1500 分钟）
     */
    public Map<String, Object> dailyBriefing() {
        String cacheKey = buildScopeKey(AiQueryContext.get());

        BriefingCacheEntry cached = briefingCache.get(cacheKey);
        if (cached != null
                && System.currentTimeMillis() - cached.createdAt < briefingTtlMinutes * 60_000L) {
            log.debug("每日简报命中缓存: cacheKey={}", cacheKey);
            return cached.result;
        }
        log.debug("每日简报未命中缓存，重新生成: cacheKey={}", cacheKey);

        Map<String, Object> result = generateBriefing();
        if (result != null && !result.isEmpty()) {
            briefingCache.put(cacheKey, new BriefingCacheEntry(result));
        }
        return result;
    }

    /** 清空简报缓存（数据批量同步完成后调用，使下次访问重新生成最新数据） */
    public void clearBriefingCache() {
        int size = briefingCache.size();
        briefingCache.clear();
        log.info("每日简报缓存已清空，清理 {} 个条目", size);
    }

    /** 简报实际生成逻辑（首次调用/缓存过期时执行）。复用 LoanAccountService.getStats 聚合，避免重复统计逻辑。 */
    private Map<String, Object> generateBriefing() {
        AiUserScope scope = AiQueryContext.get();
        Map<String, Object> stats = loanAccountService.getStats(scope.getOrgCode(), scope.getEhrNo());
        Map<String, Object> result = new HashMap<>();

        if (stats != null && !stats.isEmpty()) {
            result.put("stats", stats);

            if (llmEnabled()) {
                String prompt = "根据以下今日业务数据，生成一段100字以内的每日简报，突出关键变化：\n" +
                    stats.toString();
                String briefing = llmClient.chat(SYSTEM_PROMPT, prompt, thinkingBriefing);
                result.put("briefing", briefing != null ? briefing : buildFallbackBriefing(stats));
            } else {
                result.put("briefing", buildFallbackBriefing(stats));
            }
        }
        return result;
    }

    /** 按数据范围构造缓存 key */
    private String buildScopeKey(AiUserScope scope) {
        if (scope == null || scope.getAllowedBranchCodes() == null || scope.getAllowedBranchCodes().isEmpty()) {
            return "GLOBAL";
        }
        List<String> codes = new ArrayList<>(scope.getAllowedBranchCodes());
        java.util.Collections.sort(codes);
        return String.join(",", codes);
    }

    /**
     * 催收历程摘要
     */
    public Map<String, Object> collectionSummary(String loanAccount) {
        Map<String, Object> result = new HashMap<>();

        // 范围校验：账户 branchCode 必须在当前用户可访问范围内（杜绝越权）
        AiUserScope scope = AiQueryContext.get();
        List<String> allowed = scope != null ? scope.getAllowedBranchCodes() : null;
        if (allowed == null || allowed.isEmpty()) {
            log.warn("催收摘要缺少权限范围，拒绝访问: loanAccount={}", loanAccount);
            result.put("summary", "暂无催收记录");
            return result;
        }
        String branchCode = loanAccountRepository.findById(loanAccount)
                .map(LoanAccount::getBranchCode).orElse(null);
        if (branchCode == null || !allowed.contains(branchCode)) {
            log.warn("催收摘要越权拦截: loanAccount={}, branchCode={} 不在允许范围", loanAccount, branchCode);
            result.put("summary", "暂无催收记录");
            return result;
        }

        List<CollectionRecord> records = collectionRecordRepository.findAllByLoanAccount(loanAccount);
        log.debug("催收历程摘要: loanAccount={}, 记录数={}", loanAccount, records.size());

        if (records.isEmpty()) {
            result.put("summary", "暂无催收记录");
            return result;
        }

        // 按方法聚合
        Map<String, Long> methodCount = records.stream()
            .collect(Collectors.groupingBy(
                r -> r.getMethodText() != null ? r.getMethodText() : r.getMethod(),
                Collectors.counting()));

        long total = records.size();
        String latestMethod = records.get(0).getMethodText() != null ?
            records.get(0).getMethodText() : records.get(0).getMethod();

        if (llmEnabled()) {
            StringBuilder recordsSummary = new StringBuilder();
            for (CollectionRecord r : records) {
                recordsSummary.append(String.format("- %s (%s): %s\n",
                    r.getOperateTime() != null ? r.getOperateTime().toLocalDate() : "未知",
                    r.getMethodText() != null ? r.getMethodText() : r.getMethod(),
                    r.getResult() != null ? r.getResult() : "无"));
            }
            String prompt = String.format(
                "以下是账户 %s 的催收记录（共%d条），请用80字以内总结催收历程：\n%s",
                loanAccount, total, recordsSummary.toString());
            String summary = llmClient.chat(SYSTEM_PROMPT, prompt, thinkingSummary);
            result.put("summary", summary != null ? summary : buildFallbackSummary(total, latestMethod, methodCount));
        } else {
            result.put("summary", buildFallbackSummary(total, latestMethod, methodCount));
        }
        result.put("totalRecords", total);
        result.put("methodCount", methodCount);
        return result;
    }

    // ==================== 降级逻辑 ====================

    private boolean llmEnabled() {
        return llmClient != null && llmClient.isAvailable();
    }

    private String buildFallbackBriefing(Map<String, Object> stats) {
        long active = toLong(stats.get("activeCount"));
        long uncollected = toLong(stats.get("uncollectedCount"));
        long collecting = toLong(stats.get("collectingCount"));
        long completed = toLong(stats.get("completedCount"));
        return String.format("今日未完成催收%d笔（未催收%d笔，催收中%d笔），已完成%d笔。",
            active, uncollected, collecting, completed);
    }

    private String buildFallbackSummary(long total, String latestMethod, Map<String, Long> methodStats) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("共进行%d次催收", total));
        if (!methodStats.isEmpty()) {
            sb.append("（");
            methodStats.forEach((method, count) -> sb.append(method).append(count).append("次 "));
            sb.append("）");
        }
        sb.append(String.format("，最近一次为%s。", latestMethod));
        return sb.toString();
    }

    private long toLong(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).longValue();
        return 0;
    }
}
