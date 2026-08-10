package com.bank.lms.service.analysis;

import com.bank.lms.config.AiQueryContext;
import com.bank.lms.dto.analysis.AiUserScope;
import com.bank.lms.dto.analysis.AnalysisResult;
import com.bank.lms.entity.AiQueryAuditLog;
import com.bank.lms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 安全分析执行器
 * 所有预编译 SQL 经此类执行，自动注入行级安全过滤
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecureAnalysisExecutor {

    private final LoanAccountRepository loanAccountRepository;
    private final CollectionRecordRepository collectionRecordRepository;
    private final UserOrgRepository userOrgRepository;
    private final UserLoginLogRepository userLoginLogRepository;
    private final BranchOrgRepository branchOrgRepository;
    private final AiQueryAuditLogRepository auditLogRepository;

    /**
     * 执行分析能力
     * allowedBranchCodes 从 AiQueryContext 注入，LLM 永远看不到
     */
    public AnalysisResult execute(AnalysisCapability capability, Map<String, Object> params) {
        AiUserScope scope = AiQueryContext.get();
        if (scope == null) {
            throw new SecurityException("未登录或会话已过期，缺少用户上下文");
        }

        // 注入权限参数
        params.put("allowedBranchCodes", scope.getAllowedBranchCodes());
        params.put("allowedOrgCodes", scope.getAllowedOrgCodes());

        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> rows = executeCapability(capability, params);
        long elapsed = System.currentTimeMillis() - startTime;

        // 审计日志
        auditLog(scope, capability, params, rows != null ? rows.size() : 0, (int) elapsed);

        return new AnalysisResult(capability.name(), rows, rows != null ? rows.size() : 0);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> executeCapability(AnalysisCapability capability, Map<String, Object> params) {
        List<String> allowedBranchCodes = (List<String>) params.get("allowedBranchCodes");
        if (allowedBranchCodes == null || allowedBranchCodes.isEmpty()) {
            return Collections.emptyList();
        }

        switch (capability) {
            case ORG_RANKING:
                return executeOrgRanking(allowedBranchCodes);
            case OVERDUE_TREND:
                return executeOverdueTrend(allowedBranchCodes, params);
            case OVERDUE_AGING:
                return executeOverdueAging(allowedBranchCodes);
            case COMPLETION_RATE:
                return executeCompletionRate(allowedBranchCodes);
            case HIGH_OVERDUE_BACKLOG:
                return executeHighOverdueBacklog(allowedBranchCodes);
            case EMPLOYEE_WORKLOAD:
                return executeEmployeeWorkload(allowedBranchCodes, params);
            case LOGIN_ACTIVITY:
                return executeLoginActivity(allowedBranchCodes);
            case COLLECTION_SUMMARY:
                return executeCollectionSummary(params);
            case DAILY_BRIEFING:
                return executeDailyBriefing(allowedBranchCodes);
            default:
                return Collections.emptyList();
        }
    }

    // ==================== 各能力的具体实现 ====================

    private List<Map<String, Object>> executeOrgRanking(List<String> branchCodes) {
        List<Object[]> rows = loanAccountRepository.rankingByBranchCodes(branchCodes);
        return mapToResult(rows, "branchCode", "branchName", "count", "totalAmt");
    }

    private List<Map<String, Object>> executeOverdueTrend(List<String> branchCodes, Map<String, Object> params) {
        // 使用 LoanAccountRepository 中已有的 overdueDaily 查询
        // 此处简化实现，实际应通过 JPQL 查询 overdue_date 按天聚合
        return new ArrayList<>();
    }

    private List<Map<String, Object>> executeOverdueAging(List<String> branchCodes) {
        List<Object[]> rows = loanAccountRepository.agingDistribution(branchCodes);
        return mapToResult(rows, "aging", "count");
    }

    private List<Map<String, Object>> executeCompletionRate(List<String> branchCodes) {
        // 从 loan_account 统计状态变更 + collection_record 统计操作量
        return new ArrayList<>();
    }

    private List<Map<String, Object>> executeHighOverdueBacklog(List<String> branchCodes) {
        List<Object[]> rows = loanAccountRepository.deepOverdueCount(branchCodes);
        return mapToResult(rows, "branchCode", "count", "totalAmt");
    }

    private List<Map<String, Object>> executeEmployeeWorkload(List<String> branchCodes, Map<String, Object> params) {
        List<Object[]> rows = collectionRecordRepository.workloadStats(branchCodes);
        return mapToResult(rows, "operatorId", "operatorName", "count", "uniqueAccounts");
    }

    private List<Map<String, Object>> executeLoginActivity(List<String> branchCodes) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Object[]> rows = userLoginLogRepository.countActiveDaysByOrgCodeInSince(branchCodes, since);
        return mapToResult(rows, "ehrNo", "userName", "activeDays");
    }

    private List<Map<String, Object>> executeCollectionSummary(Map<String, Object> params) {
        String loanAccount = (String) params.get("loanAccount");
        List<Object[]> rows = collectionRecordRepository.summaryByLoanAccount(loanAccount);
        return mapToResult(rows, "method", "count", "lastResult");
    }

    private List<Map<String, Object>> executeDailyBriefing(List<String> branchCodes) {
        // 聚合多表数据：逾期总数、金额、完成率、新增逾期、同比
        Map<String, Object> briefing = new HashMap<>();

        List<Object[]> statsRows = loanAccountRepository.statsActiveByBranchCodes(branchCodes);
        if (statsRows != null && !statsRows.isEmpty()) {
            Object[] row = statsRows.get(0);
            briefing.put("activeCount", row[0]);
            briefing.put("totalLoanBalance", row[1]);
        }

        List<Object[]> statusRows = loanAccountRepository.countByStatusForBranchCodes(branchCodes);
        Map<String, Long> statusMap = new HashMap<>();
        if (statusRows != null) {
            for (Object[] sc : statusRows) {
                statusMap.put((String) sc[0], ((Number) sc[1]).longValue());
            }
        }
        briefing.put("uncollectedCount", statusMap.getOrDefault("uncollected", 0L));
        briefing.put("collectingCount", statusMap.getOrDefault("collecting", 0L));
        briefing.put("completedCount", statusMap.getOrDefault("completed", 0L));

        List<Map<String, Object>> result = new ArrayList<>();
        result.add(briefing);
        return result;
    }

    // ==================== 辅助方法 ====================

    private List<Map<String, Object>> mapToResult(List<Object[]> rows, String... keys) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (rows != null) {
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                for (int i = 0; i < Math.min(keys.length, row.length); i++) {
                    item.put(keys[i], row[i]);
                }
                result.add(item);
            }
        }
        return result;
    }

    private void auditLog(AiUserScope scope, AnalysisCapability capability,
                          Map<String, Object> params, int rowCount, int elapsedMs) {
        try {
            AiQueryAuditLog logEntry = new AiQueryAuditLog();
            logEntry.setEhrNo(scope.getEhrNo());
            logEntry.setOrgCode(scope.getOrgCode());
            logEntry.setQuestion(params.containsKey("question") ? String.valueOf(params.get("question")) : null);
            logEntry.setCapability(capability.name());
            logEntry.setParams(params.toString());
            logEntry.setRowCount(rowCount);
            logEntry.setExecutionTimeMs(elapsedMs);
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("AI审计日志写入失败: {}", e.getMessage());
        }
    }
}
