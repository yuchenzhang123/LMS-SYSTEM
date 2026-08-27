package com.bank.lms.integration;

import com.bank.lms.config.AiQueryContext;
import com.bank.lms.dto.analysis.AiUserScope;
import com.bank.lms.dto.analysis.AnalysisResult;
import com.bank.lms.entity.CollectionRecord;
import com.bank.lms.entity.LoanAccount;
import com.bank.lms.repository.CollectionRecordRepository;
import com.bank.lms.repository.LoanAccountRepository;
import com.bank.lms.service.analysis.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI引擎集成测试 — 自动制造测试数据，测试后回滚
 * 运行: mvn test -Dtest=AiCopilotIntegrationTest -Dspring.profiles.active=test
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AI引擎集成测试")
class AiCopilotIntegrationTest {

    private static final String TEST_ORG = "AI_TEST_ORG";
    private static final String TEST_BRANCH = "AI_TEST_BRANCH";

    @Autowired private CopilotService copilotService;
    @Autowired private SecureAnalysisExecutor secureAnalysisExecutor;
    @Autowired private AccountPriorityScorer priorityScorer;
    @Autowired private LoanAccountRepository loanAccountRepository;
    @Autowired private CollectionRecordRepository collectionRecordRepository;

    private AiUserScope testScope;
    private List<String> createdAccountIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 清理上次可能残留的测试数据（防止上次强制停止导致回滚失败）
        cleanupLeftoverData();

        testScope = new AiUserScope();
        testScope.setEhrNo("test001");
        testScope.setOrgCode(TEST_ORG);
        testScope.setUserRole("admin");
        testScope.setAllowedOrgCodes(Collections.singletonList(TEST_ORG));
        testScope.setAllowedBranchCodes(Arrays.asList(TEST_BRANCH, TEST_ORG));
        AiQueryContext.set(testScope);
    }

    @AfterEach
    void tearDown() {
        AiQueryContext.clear();
        createdAccountIds.clear();
    }

    /** 删除所有以 AI_ 开头的测试账户及其催收记录 */
    private void cleanupLeftoverData() {
        try {
            List<LoanAccount> leftovers = loanAccountRepository.findAll().stream()
                .filter(a -> a.getLoanAccount() != null && a.getLoanAccount().startsWith("AI_"))
                .collect(java.util.stream.Collectors.toList());
            for (LoanAccount a : leftovers) {
                // 先删催收记录
                List<CollectionRecord> records = collectionRecordRepository.findByLoanAccountOrderByOperateTimeDesc(a.getLoanAccount());
                if (records != null) collectionRecordRepository.deleteAll(records);
                loanAccountRepository.delete(a);
            }
            if (!leftovers.isEmpty()) {
                System.out.println("[清理] 删除了 " + leftovers.size() + " 个残留测试账户");
            }
        } catch (Exception e) {
            System.out.println("[清理] 忽略: " + e.getMessage());
        }
    }

    // ==================== 测试数据工厂 ====================

    private LoanAccount createAccount(String id, int overdueDays, double amount, String status) {
        LoanAccount a = new LoanAccount();
        a.setLoanAccount(id);
        a.setCustomerId("CUST_" + id);
        a.setCustomerName("测试客户" + id);
        a.setProductCode("XFD001");
        a.setOverdueDays(overdueDays);
        a.setTotalOverdueAmount(new BigDecimal(amount));
        a.setLoanBalance(new BigDecimal(amount * 1.5));
        a.setStatus(status);
        a.setBranchCode(TEST_BRANCH);
        a.setBranchName("测试分支行");
        a.setLoanDate(LocalDate.now().minusMonths(6));
        a.setIsDeleted(0);
        loanAccountRepository.save(a);
        createdAccountIds.add(id);
        return a;
    }

    private CollectionRecord createRecord(String loanAccount, String method, String result, int daysAgo) {
        CollectionRecord r = new CollectionRecord();
        // record_id 列 VARCHAR(32)，nanoTime 最多 19 位会超长，收窄到 10 位后缀
        r.setRecordId("REC_" + loanAccount + "_" + Math.abs(System.nanoTime() % 10000000000L));
        r.setLoanAccount(loanAccount);
        r.setCustomerId("CUST_" + loanAccount);
        r.setMethod(method);
        r.setMethodText(method.equals("phone") ? "电话催收" : "短信催收");
        r.setResult(result);
        r.setOperatorId("OP001");
        r.setOperatorName("测试催收员");
        r.setOperateTime(LocalDateTime.now().minusDays(daysAgo));
        collectionRecordRepository.save(r);
        return r;
    }

    // ==================== 优先级评分测试 ====================

    @Test
    @DisplayName("AI-01 高风险账户优先级最高")
    void priorityHighRisk() {
        LoanAccount high = createAccount("AI_HIGH_RISK", 120, 1000000, "collecting");
        LoanAccount low = createAccount("AI_LOW_RISK", 3, 5000, "uncollected");

        CollectionRecord recentRecord = createRecord("AI_LOW_RISK", "phone", "客户承诺还款", 1);

        double highScore = priorityScorer.score(high, null);
        double lowScore = priorityScorer.score(low, recentRecord);

        System.out.printf("高风险账户得分: %.1f, 低风险账户得分: %.1f%n", highScore, lowScore);
        assertTrue(highScore > lowScore,
            "高风险应排在低风险之前: " + highScore + " vs " + lowScore);
    }

    @Test
    @DisplayName("AI-02 从未催收的账户得分高")
    void priorityNoCollection() {
        LoanAccount neverCollected = createAccount("AI_NEVER", 90, 500000, "uncollected");

        double score = priorityScorer.score(neverCollected, null);
        System.out.println("从未催收得分: " + score);
        assertTrue(score >= 60, "从未催收+高逾期应有较高优先级, 实际=" + score);
    }

    // ==================== 分析能力执行测试 ====================

    @Test
    @DisplayName("AI-03 逾期账龄分布")
    void overdueAging() {
        createAccount("AI_AGING_7", 5, 10000, "collecting");    // 1-7天
        createAccount("AI_AGING_30", 20, 50000, "collecting");  // 8-30天
        createAccount("AI_AGING_60", 45, 100000, "collecting"); // 31-60天
        createAccount("AI_AGING_90", 80, 200000, "collecting"); // 60天+

        Map<String, Object> params = new HashMap<>();
        AnalysisResult result = secureAnalysisExecutor.execute(AnalysisCapability.OVERDUE_AGING, params);

        System.out.println("账龄分布: ");
        for (Map<String, Object> row : result.getRows()) {
            System.out.println("  " + row.get("aging") + " → " + row.get("count") + " 笔");
        }
        assertTrue(result.getRowCount() >= 4);
    }

    @Test
    @DisplayName("AI-04 机构排名")
    void orgRanking() {
        createAccount("AI_RANK_1", 30, 200000, "collecting");
        createAccount("AI_RANK_2", 60, 500000, "collecting");
        createAccount("AI_RANK_3", 15, 80000, "uncollected");

        AnalysisResult result = secureAnalysisExecutor.execute(AnalysisCapability.ORG_RANKING, new HashMap<>());

        System.out.println("机构排名: " + result.getRowCount() + " 条");
        for (Map<String, Object> row : result.getRows()) {
            System.out.println("  " + row.get("branchCode") + " → "
                + row.get("count") + "笔 / " + row.get("totalAmt") + "元");
        }
        assertTrue(result.getRowCount() >= 1);
    }

    @Test
    @DisplayName("AI-05 深度逾期统计")
    void highOverdueBacklog() {
        createAccount("AI_DEEP_1", 31, 100000, "collecting");
        createAccount("AI_DEEP_2", 45, 200000, "collecting");
        createAccount("AI_DEEP_3", 10, 50000, "collecting"); // 不算深度逾期

        AnalysisResult result = secureAnalysisExecutor.execute(AnalysisCapability.HIGH_OVERDUE_BACKLOG, new HashMap<>());

        System.out.println("深度逾期(>30天): " + result.getRowCount() + " 条");
        assertTrue(result.getRowCount() >= 1);
    }

    @Test
    @DisplayName("AI-06 员工工作量统计")
    void employeeWorkload() {
        createAccount("AI_WORK_1", 20, 100000, "collecting");
        createRecord("AI_WORK_1", "phone", "已联系", 1);
        createRecord("AI_WORK_1", "visit", "上门催收", 3);
        createRecord("AI_WORK_1", "sms", "短信提醒", 5);

        AnalysisResult result = secureAnalysisExecutor.execute(AnalysisCapability.EMPLOYEE_WORKLOAD, new HashMap<>());

        System.out.println("员工工作量: " + result.getRowCount() + " 人");
        for (Map<String, Object> row : result.getRows()) {
            System.out.println("  " + row.get("operatorId") + "(" + row.get("operatorName") + ")"
                + " → " + row.get("count") + "次 / " + row.get("uniqueAccounts") + "户");
        }
        assertTrue(result.getRowCount() >= 1);
    }

    // ==================== AI 对话测试 ====================

    @Test
    @DisplayName("AI-07 AI问答 — LLM关闭时降级")
    void aiChatFallback() {
        createAccount("AI_CHAT_1", 45, 500000, "collecting");

        Map<String, Object> answer = copilotService.ask("逾期趋势如何");
        System.out.println("AI问答结果: " + answer);

        assertNotNull(answer);
        assertNotNull(answer.get("answer"));
        assertFalse(((String) answer.get("answer")).isEmpty());
        System.out.println("回答: " + answer.get("answer"));
    }

    @Test
    @DisplayName("AI-08 每日简报")
    void dailyBriefing() {
        createAccount("AI_BRIEF_1", 10, 100000, "uncollected");
        createAccount("AI_BRIEF_2", 30, 300000, "collecting");
        createAccount("AI_BRIEF_3", 0, 200000, "completed");

        Map<String, Object> briefing = copilotService.dailyBriefing();
        System.out.println("每日简报: " + briefing);

        assertNotNull(briefing);
        if (briefing.containsKey("briefing")) {
            System.out.println("简报文字: " + briefing.get("briefing"));
        }
        if (briefing.containsKey("stats")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = (Map<String, Object>) briefing.get("stats");
            System.out.println("统计数据: activeCount=" + stats.get("activeCount"));
        }
    }

    @Test
    @DisplayName("AI-09 催收历程摘要")
    void collectionSummary() {
        createAccount("AI_SUMMARY_1", 30, 200000, "collecting");
        createRecord("AI_SUMMARY_1", "phone", "客户称下周还款", 1);
        createRecord("AI_SUMMARY_1", "sms", "已发送提醒短信", 3);
        createRecord("AI_SUMMARY_1", "phone", "电话无人接听", 7);
        createRecord("AI_SUMMARY_1", "visit", "上门催收，客户承诺本月还清", 10);

        Map<String, Object> summary = copilotService.collectionSummary("AI_SUMMARY_1");
        System.out.println("催收摘要: " + summary);

        assertNotNull(summary);
        assertNotNull(summary.get("summary"));
        assertNotNull(summary.get("totalRecords"));
        assertEquals(4L, ((Number) summary.get("totalRecords")).longValue());
        System.out.println("总记录: " + summary.get("totalRecords") + ", 摘要: " + summary.get("summary"));
    }

    @Test
    @DisplayName("AI-10 催收摘要 — 无记录账户")
    void collectionSummaryEmpty() {
        createAccount("AI_SUMMARY_EMPTY", 5, 10000, "uncollected");

        Map<String, Object> summary = copilotService.collectionSummary("AI_SUMMARY_EMPTY");
        assertNotNull(summary);
        assertEquals("暂无催收记录", summary.get("summary"));
        System.out.println("空记录: " + summary.get("summary"));
    }

    // ==================== 安全测试 ====================

    @Test
    @DisplayName("AI-11 缺少用户上下文时拒绝执行")
    void noContextRejected() {
        AiQueryContext.clear(); // 清除上下文

        assertThrows(SecurityException.class, () -> {
            secureAnalysisExecutor.execute(AnalysisCapability.ORG_RANKING, new HashMap<>());
        });
        System.out.println("✅ 无上下文被正确拒绝");
    }
}
