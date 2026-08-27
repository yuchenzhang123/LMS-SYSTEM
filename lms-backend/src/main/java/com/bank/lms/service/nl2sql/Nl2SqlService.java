package com.bank.lms.service.nl2sql;

import com.bank.lms.dto.analysis.AiUserScope;
import com.bank.lms.entity.AiQueryAuditLog;
import com.bank.lms.repository.AiQueryAuditLogRepository;
import com.bank.lms.service.llm.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NL2SQL 编排：LLM 规划（JSON 两步法）→ 安全守卫 → 执行 → 修正轮 → 润色 → 审计。
 *
 * 这里没有引入 Spring AI / LangChain4j 等框架——它们要求 JDK 17+，与项目 Java 1.8 冲突；
 * LLM 调用复用现有 {@link LlmClient}（内网 32B 已是 OpenAI 兼容接口），编排是项目独有业务逻辑，
 * 故手写轻量编排，SQL 解析复用 JSqlParser（见 {@link SqlSafetyGuard}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Nl2SqlService {

    private final LlmClient llmClient;
    private final SchemaRegistry schemaRegistry;
    private final SqlSafetyGuard sqlSafetyGuard;
    private final NamedParameterJdbcTemplate mainJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AiQueryAuditLogRepository auditLogRepository;

    /** 润色结果时取前多少行喂给 LLM */
    private static final int POLISH_ROW_LIMIT = 20;

    // ==================== Step1：LLM 规划 ====================

    /**
     * 让 LLM 输出规划 JSON。返回 null 表示 LLM 不可用或规划失败（调用方降级）。
     */
    public Nl2SqlPlan plan(String question) {
        String systemPrompt = buildPlanPrompt();
        String raw = llmClient.chatJson(systemPrompt, question);
        if (raw == null) {
            return null;
        }
        try {
            return parsePlan(raw);
        } catch (Exception e) {
            log.debug("LLM 规划 JSON 解析失败，进入修正轮: {}", e.getMessage());
            return planWithCorrection(systemPrompt, question, e.getMessage());
        }
    }

    private Nl2SqlPlan planWithCorrection(String systemPrompt, String question, String error) {
        String raw = llmClient.chatJson(systemPrompt,
            "你上次的输出无法解析为 JSON：" + error + "\n请严格输出单个 JSON 对象，不要多余文字。\n问题：" + question);
        if (raw == null) {
            return null;
        }
        try {
            return parsePlan(raw);
        } catch (Exception e) {
            log.warn("LLM 规划修正轮仍失败: {}", e.getMessage());
            return null;
        }
    }

    private Nl2SqlPlan parsePlan(String raw) throws Exception {
        String json = JsonExtractor.extractJson(raw);
        return objectMapper.readValue(json, Nl2SqlPlan.class);
    }

    // ==================== Step2：执行（含修正轮） ====================

    /**
     * 执行 NL2SQL 自由查询：安全守卫 → 执行 → 失败修正一轮 → 润色 → 审计。
     */
    public Nl2SqlResult executeAndAnswer(String sql, String question, AiUserScope scope) {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> rows = null;
        List<String> columns = null;
        String rewrittenSql = null;
        String errorMsg = null;

        try {
            SafeQuery safe = sqlSafetyGuard.enforce(sql, scope);
            rewrittenSql = safe.getRewrittenSql();
            rows = mainJdbcTemplate.queryForList(rewrittenSql, safe.getParams());
            columns = extractColumns(rows);
        } catch (Exception firstError) {
            // 修正轮：把守卫拒绝原因或 SQL 异常回喂 LLM 重新生成一次
            log.debug("NL2SQL 首轮失败，进入修正轮: {}", firstError.getMessage());
            String fixedSql = correctSql(question, sql, firstError.getMessage());
            if (fixedSql != null && !fixedSql.trim().isEmpty()) {
                try {
                    SafeQuery safe = sqlSafetyGuard.enforce(fixedSql, scope);
                    rewrittenSql = safe.getRewrittenSql();
                    rows = mainJdbcTemplate.queryForList(rewrittenSql, safe.getParams());
                    columns = extractColumns(rows);
                } catch (Exception secondError) {
                    errorMsg = secondError.getMessage();
                }
            } else {
                errorMsg = firstError.getMessage();
            }
        }

        Nl2SqlResult result = new Nl2SqlResult();
        result.setIntent("nl2sql");
        result.setColumns(columns);
        result.setRows(rows == null ? Collections.<Map<String, Object>>emptyList() : rows);
        result.setRowCount(rows == null ? 0 : rows.size());
        result.setSuccess(rows != null);

        if (rows == null || rows.isEmpty()) {
            result.setAnswerText(errorMsg != null
                ? "查询失败：" + errorMsg
                : "未查询到相关数据");
        } else {
            result.setAnswerText(polish(question, rows, rows.size()));
        }

        audit(question, sql, rewrittenSql, scope, result.getRowCount(),
            (int) (System.currentTimeMillis() - start), result.isSuccess());
        return result;
    }

    private String correctSql(String question, String originalSql, String error) {
        String prompt = "你生成的 SQL 有误或违规，原因：" + error
            + "\n请修正 SQL 后严格输出单个 JSON 对象。\n问题：" + question;
        String raw = llmClient.chatJson(buildPlanPrompt(), prompt);
        if (raw == null) {
            return null;
        }
        try {
            Nl2SqlPlan p = parsePlan(raw);
            return p.getSql();
        } catch (Exception e) {
            log.warn("SQL 修正轮解析失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== Step3：润色 ====================

    /**
     * 把查询结果摘要回喂 LLM 生成自然语言回答。
     */
    public String polish(String question, List<Map<String, Object>> rows, int rowCount) {
        String systemPrompt = "你是银行贷后催收数据分析助手。根据查询结果用简洁中文回答用户，突出关键数字，最多3句话，不要编造数据。";
        StringBuilder summary = new StringBuilder();
        int show = Math.min(POLISH_ROW_LIMIT, rows.size());
        summary.append("[");
        for (int i = 0; i < show; i++) {
            if (i > 0) {
                summary.append(", ");
            }
            summary.append(rows.get(i).toString());
        }
        summary.append("]");
        String user = "问题：" + question + "\n查询结果（共" + rowCount + "行，展示前" + show + "行）：" + summary.toString();
        String answer = llmClient.chat(systemPrompt, user, false);
        return answer != null && !answer.trim().isEmpty()
            ? answer
            : "共查询到 " + rowCount + " 条记录";
    }

    // ==================== 辅助 ====================

    private List<String> extractColumns(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        // 用 LinkedHashMap 保序去重
        List<String> columns = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            for (String key : row.keySet()) {
                if (!columns.contains(key)) {
                    columns.add(key);
                }
            }
        }
        return columns;
    }

    private void audit(String question, String rawSql, String rewrittenSql, AiUserScope scope,
                       int rowCount, int elapsedMs, boolean success) {
        try {
            AiQueryAuditLog entry = new AiQueryAuditLog();
            entry.setEhrNo(scope.getEhrNo());
            entry.setOrgCode(scope.getOrgCode());
            entry.setQuestion(question);
            entry.setCapability("nl2sql");
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("rawSql", rawSql);
            p.put("rewrittenSql", rewrittenSql);
            p.put("success", success);
            entry.setParams(objectMapper.writeValueAsString(p));
            entry.setRowCount(rowCount);
            entry.setExecutionTimeMs(elapsedMs);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("NL2SQL 审计日志写入失败: {}", e.getMessage());
        }
    }

    private String buildPlanPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是银行贷后催收数据分析助手。根据用户问题和表结构，判断意图并输出一个 JSON。\n\n");
        sb.append("可用表结构（列名用英文原样，中文为含义）：\n");
        sb.append(schemaRegistry.buildPrompt());
        sb.append("\n\nSQL 硬性规则：\n");
        sb.append("1) 只允许单条 SELECT，禁止 INSERT/UPDATE/DELETE/DROP/UNION/子查询/注释/分号/反引号\n");
        sb.append("2) 表名与列名只能来自上表，列名用英文原样\n");
        sb.append("3) 聚合用标准 COUNT/SUM/AVG/MIN/MAX；空值用 COALESCE（不是 IFNULL）；日期用标准函数\n");
        sb.append("4) 不要写 WHERE branch_code/org_code 过滤，系统会自动按权限补充（重要！）\n");
        sb.append("5) 主表不要用别名；结果加 LIMIT 100 即可\n");
        sb.append("\n只输出一个 JSON 对象，不要 markdown 和多余文字：\n");
        sb.append("查询数据：{\"intent\":\"nl2sql\",\"sql\":\"SELECT ...\"}\n");
        sb.append("闲聊/问候（不查库）：{\"intent\":\"chat\"}\n");
        return sb.toString();
    }
}
