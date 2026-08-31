package com.bank.lms.integration;

import com.bank.lms.dto.analysis.AiUserScope;
import com.bank.lms.entity.CollectionRecord;
import com.bank.lms.entity.LoanAccount;
import com.bank.lms.entity.UserOrg;
import com.bank.lms.repository.CollectionRecordRepository;
import com.bank.lms.repository.LoanAccountRepository;
import com.bank.lms.repository.UserOrgRepository;
import com.bank.lms.service.nl2sql.Nl2SqlResult;
import com.bank.lms.service.nl2sql.Nl2SqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NL2SQL 受控 JOIN 集成测试 — 真实守卫重写 + 真实 DB 执行（本地 MySQL），测试后回滚。
 *
 * 验证「哪个员工催收效率最高」这类跨表问题：LLM 生成 collection_record JOIN user_org 的 SQL，
 * 守卫逐表注入权限（collection_record 借 loan_account 注 branch_code + 软删；user_org 注 org_code），
 * 最终在真实数据库执行并返回正确结果。
 *
 * 运行: mvn test -Dtest=Nl2SqlJoinIntegrationTest（需按 local-integration-test-runtime 记忆覆盖数据源到本地 MySQL）
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("NL2SQL 受控 JOIN 集成测试")
class Nl2SqlJoinIntegrationTest {

    private static final String ORG = "JOIN_TEST_ORG";
    private static final String BRANCH = "JOIN_TEST_BRANCH";

    @Autowired private Nl2SqlService nl2SqlService;
    @Autowired private LoanAccountRepository loanAccountRepository;
    @Autowired private CollectionRecordRepository collectionRecordRepository;
    @Autowired private UserOrgRepository userOrgRepository;

    private AiUserScope scope;

    @BeforeEach
    void setUp() {
        scope = new AiUserScope();
        scope.setEhrNo("join_test_001");
        scope.setOrgCode(ORG);
        scope.setUserRole("admin");
        scope.setAllowedOrgCodes(Collections.singletonList(ORG));
        scope.setAllowedBranchCodes(Collections.singletonList(BRANCH));
    }

    private LoanAccount account(String id, String status) {
        LoanAccount a = new LoanAccount();
        a.setLoanAccount(id);
        a.setCustomerId("JOIN_CUST_" + id);
        a.setCustomerName("测试客户" + id);
        a.setProductCode("XFD001");
        a.setOverdueDays(30);
        a.setTotalOverdueAmount(new BigDecimal(10000));
        a.setLoanBalance(new BigDecimal(15000));
        a.setStatus(status);
        a.setBranchCode(BRANCH);
        a.setBranchName("测试分支行");
        a.setIsDeleted(0);
        return loanAccountRepository.saveAndFlush(a);
    }

    private CollectionRecord record(String loanAccount, String operatorId, String operatorName, String result) {
        CollectionRecord r = new CollectionRecord();
        r.setRecordId("JOIN_REC_" + loanAccount + "_" + Math.abs(System.nanoTime() % 10000000000L));
        r.setLoanAccount(loanAccount);
        r.setCustomerId("JOIN_CUST_" + loanAccount);
        r.setCustomerName("测试客户" + loanAccount);
        r.setMethod("phone");
        r.setMethodText("电话催收");
        r.setResult(result);
        r.setOperatorId(operatorId);
        r.setOperatorName(operatorName);
        r.setOperateTime(LocalDateTime.now());
        r.setIsDeleted(0);
        return collectionRecordRepository.saveAndFlush(r);
    }

    private void user(String ehrNo, String name) {
        UserOrg u = new UserOrg();
        u.setEhrNo(ehrNo);
        u.setUserName(name);
        u.setOrgCode(ORG);
        u.setOrgName("测试机构");
        u.setStatus("active");
        userOrgRepository.saveAndFlush(u);
    }

    @Test
    @DisplayName("催收员效率多表 JOIN：守卫逐表注入 + 真实执行返回正确结果")
    void collectionEfficiencyJoin() {
        // 造数据：业务员A 催 2 个账户（其一 completed），业务员B 催 1 个账户（collecting）
        LoanAccount a1 = account("JOIN_LA1", "completed");
        LoanAccount a2 = account("JOIN_LA2", "collecting");
        user("JOIN_OP_A", "业务员A");
        user("JOIN_OP_B", "业务员B");
        record(a1.getLoanAccount(), "JOIN_OP_A", "业务员A", "客户承诺还款");
        record(a2.getLoanAccount(), "JOIN_OP_A", "业务员A", "已发送提醒短信");
        record(a2.getLoanAccount(), "JOIN_OP_B", "业务员B", "电话未接通");

        // 模拟 LLM 生成的多表 JOIN SQL（催收员按催收次数排行）
        String sql = "SELECT u.user_name AS emp_name, COUNT(*) AS cnt "
            + "FROM collection_record c JOIN user_org u ON c.operator_id = u.ehr_no "
            + "GROUP BY u.user_name ORDER BY cnt DESC LIMIT 20";

        Nl2SqlResult result = nl2SqlService.executeAndAnswer(sql, "哪个员工催收效率最高", scope);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "受控 JOIN 查询应执行成功，errorMsg=" + result.getErrorMsg());
        assertEquals(2, result.getRowCount(), "应返回 2 名催收员");

        List<Map<String, Object>> rows = result.getRows();
        // 第一行应为催收次数最多的业务员A
        Map<String, Object> top = rows.get(0);
        assertEquals("业务员A", top.get("emp_name"));
        assertEquals(2L, ((Number) top.get("cnt")).longValue());
    }

    @Test
    @DisplayName("越权表 JOIN 时守卫拒绝（非白名单表）")
    void unknownJoinTableRejected() {
        // JOIN 一个不在白名单的表，守卫应拒绝并返回失败（不抛异常，走友好降级）
        String sql = "SELECT c.record_id FROM collection_record c JOIN hacker_table h ON c.loan_account = h.x";
        Nl2SqlResult result = nl2SqlService.executeAndAnswer(sql, "越权查询", scope);

        assertNotNull(result);
        assertTrue(!result.isSuccess(), "未知 JOIN 表应被守卫拒绝");
    }
}
