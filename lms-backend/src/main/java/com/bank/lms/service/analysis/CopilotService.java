package com.bank.lms.service.analysis;

import com.bank.lms.config.AiQueryContext;
import com.bank.lms.dto.analysis.AiUserScope;
import com.bank.lms.dto.analysis.AnalysisResult;
import com.bank.lms.entity.CollectionRecord;
import com.bank.lms.repository.CollectionRecordRepository;
import com.bank.lms.service.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 助手服务
 * 提供：AI 问答、每日简报、催收历程摘要
 * LLM 只负责"理解意图"和"文字表达"，SQL 由 SecureAnalysisExecutor 执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopilotService {

    private final LlmClient llmClient;
    private final SecureAnalysisExecutor secureAnalysisExecutor;
    private final CollectionRecordRepository collectionRecordRepository;

    private static final String SYSTEM_PROMPT =
        "你是一个银行贷后催收管理系统的数据分析助手。你的职责是：\n" +
        "1. 理解用户用自然语言提出的业务问题\n" +
        "2. 从分析结果中选择最相关的能力来回答\n" +
        "3. 将数据转化为简洁易懂的中文分析，每次回答不超过150字\n" +
        "4. 不要编造数据，所有数据必须来自给出的分析结果\n" +
        "5. 可用分析能力：\n" + AnalysisCapability.buildCapabilityDescriptionForLlm();

    /**
     * AI 问答
     */
    public Map<String, Object> ask(String question) {
        AiUserScope scope = AiQueryContext.get();
        Map<String, Object> result = new HashMap<>();
        result.put("question", question);

        // 1. LLM 意图识别：用户想问什么
        String capabilityName = detectIntent(question);
        AnalysisCapability capability = AnalysisCapability.findByName(capabilityName);

        if (capability == null) {
            // 无法匹配到能力，返回通用回答
            result.put("answer", llmEnabled() ?
                llmClient.chat(SYSTEM_PROMPT, question) :
                "抱歉，我暂时无法理解您的问题。您可以尝试问：逾期趋势、机构排名、员工工作量、催收完成率等。");
            return result;
        }

        // 2. 执行预编译分析（LLM 不参与 SQL）
        Map<String, Object> params = new HashMap<>();
        params.put("question", question);
        AnalysisResult data = secureAnalysisExecutor.execute(capability, params);

        // 3. LLM 将结果变成人话
        String answer;
        if (llmEnabled()) {
            String prompt = String.format(
                "用户问题：%s\n可用的分析结果（JSON）：%s\n请根据数据回答用户问题，简洁专业，不超过150字。",
                question, data.getRows().toString());
            answer = llmClient.chat(SYSTEM_PROMPT, prompt);
        }
        if (answer == null || answer.isEmpty()) {
            answer = fallbackAnswer(capability, data);
        }

        result.put("answer", answer != null ? answer : "暂无分析结果");
        result.put("capability", capability.name());
        result.put("data", data.getRows());
        return result;
    }

    /**
     * 每日简报
     */
    public Map<String, Object> dailyBriefing() {
        Map<String, Object> result = new HashMap<>();

        AnalysisResult data = secureAnalysisExecutor.execute(
            AnalysisCapability.DAILY_BRIEFING, new HashMap<>());

        if (!data.getRows().isEmpty()) {
            result.put("stats", data.getRows().get(0));

            if (llmEnabled()) {
                String prompt = "根据以下今日业务数据，生成一段100字以内的每日简报，突出关键变化：\n" +
                    data.getRows().get(0).toString();
                String briefing = llmClient.chat(SYSTEM_PROMPT, prompt);
                result.put("briefing", briefing != null ? briefing : buildFallbackBriefing(data.getRows().get(0)));
            } else {
                result.put("briefing", buildFallbackBriefing(data.getRows().get(0)));
            }
        }
        return result;
    }

    /**
     * 催收历程摘要
     */
    public Map<String, Object> collectionSummary(String loanAccount) {
        Map<String, Object> result = new HashMap<>();

        List<CollectionRecord> records = collectionRecordRepository.findAllByLoanAccount(loanAccount);

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
            String summary = llmClient.chat(SYSTEM_PROMPT, prompt);
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

    private String detectIntent(String question) {
        if (!llmEnabled()) return null;

        String prompt = "用户问题：" + question + "\n" +
            "请从以下能力中选择最适合回答此问题的一个，只返回能力名称（如 ORG_RANKING），不要返回其他内容：\n" +
            AnalysisCapability.buildCapabilityDescriptionForLlm();
        String response = llmClient.chat(SYSTEM_PROMPT, prompt);
        return response != null ? response.trim() : null;
    }

    private String fallbackAnswer(AnalysisCapability capability, AnalysisResult data) {
        return String.format("以下是%s数据，共%d条记录。", capability.getDisplayName(), data.getRowCount());
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
