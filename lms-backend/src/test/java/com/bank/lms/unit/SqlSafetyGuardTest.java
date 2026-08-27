package com.bank.lms.unit;

import com.bank.lms.dto.analysis.AiUserScope;
import com.bank.lms.service.nl2sql.SafeQuery;
import com.bank.lms.service.nl2sql.SchemaRegistry;
import com.bank.lms.service.nl2sql.SqlGuardException;
import com.bank.lms.service.nl2sql.SqlSafetyGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

/**
 * 单元测试 — SQL 安全守卫（NL2SQL 零越权核心）。
 * 基于 JSqlParser 的真实 AST 校验，覆盖拒绝类与注入类。
 */
@DisplayName("SQL安全守卫 (单元测试)")
class SqlSafetyGuardTest {

    private SqlSafetyGuard guard;

    @BeforeEach
    void setUp() {
        guard = new SqlSafetyGuard(new SchemaRegistry());
        // @Value 字段注入，纯单测用反射回填默认值
        ReflectionTestUtils.setField(guard, "maxRows", 1000L);
        ReflectionTestUtils.setField(guard, "failClosed", true);
    }

    /** 构造带指定机构权限的范围 */
    private AiUserScope scope(String... branchCodes) {
        AiUserScope s = new AiUserScope();
        s.setEhrNo("test001");
        s.setOrgCode("TEST_ORG");
        s.setAllowedBranchCodes(Arrays.asList(branchCodes));
        s.setAllowedOrgCodes(Collections.singletonList("TEST_ORG"));
        return s;
    }

    // ==================== 拒绝类 ====================

    @Test @DisplayName("拒绝危险/非法 SQL")
    void rejectsDangerousSql() {
        AiUserScope s = scope("B001");
        String[] dangerous = {
            "INSERT INTO loan_account VALUES (1)",
            "DELETE FROM loan_account",
            "UPDATE loan_account SET status = 'x'",
            "DROP TABLE loan_account",
            "SELECT * FROM loan_account; DELETE FROM loan_account",
            "SELECT loan_account FROM loan_account UNION SELECT loan_account FROM collection_record",
            "SELECT loan_account FROM loan_account WHERE overdue_days > (SELECT AVG(overdue_days) FROM loan_account)",
            "SELECT overdue_days FROM loan_account WHERE overdue_days = SLEEP(5)",
            "SELECT * FROM loan_account INTO OUTFILE '/tmp/x'",
            "SELECT `loan_account` FROM loan_account",
            "SELECT \"loan_account\" FROM loan_account",
            "SELECT * FROM hacker_table",
            "SELECT la.loan_account FROM loan_account la",
            "SELECT loan_account FROM loan_account JOIN collection_record ON loan_account.loan_account = collection_record.loan_account"
        };
        for (String sql : dangerous) {
            assertThatThrownBy(() -> guard.enforce(sql, s))
                .as("应拒绝: %s", sql)
                .isInstanceOf(SqlGuardException.class);
        }
    }

    @Test @DisplayName("拒绝空 SQL / 空权限上下文")
    void rejectsEmpty() {
        assertThatThrownBy(() -> guard.enforce("", scope("B001")))
            .isInstanceOf(SqlGuardException.class);
        assertThatThrownBy(() -> guard.enforce(null, scope("B001")))
            .isInstanceOf(SqlGuardException.class);
        assertThatThrownBy(() -> guard.enforce("SELECT * FROM loan_account", null))
            .isInstanceOf(SqlGuardException.class);
    }

    // ==================== 注入类 ====================

    @Test @DisplayName("loan_account 注入 branch_code + 软删 + LIMIT")
    void injectBranchFilterAndSoftDelete() {
        SafeQuery q = guard.enforce(
            "SELECT loan_account, customer_name FROM loan_account WHERE overdue_days > 30",
            scope("B001", "B002"));

        assertThat(q.getRewrittenSql())
            .contains("branch_code IN (:branchCodes)")
            .contains("is_deleted = 0")
            .contains("LIMIT 1000");
        assertThat(q.getParams()).containsKey("branchCodes");
    }

    @Test @DisplayName("原 WHERE 用括号包裹，防 OR 优先级绕过")
    void wrapsOriginalWhere() {
        SafeQuery q = guard.enforce(
            "SELECT loan_account FROM loan_account WHERE overdue_days > 30 OR status = 'uncollected'",
            scope("B001"));

        assertThat(q.getRewrittenSql())
            .contains("(overdue_days > 30 OR status = 'uncollected')");
    }

    @Test @DisplayName("user_org 注入 org_code（无软删）")
    void injectOrgFilter() {
        SafeQuery q = guard.enforce("SELECT ehr_no, user_name FROM user_org", scope("B001"));

        assertThat(q.getRewrittenSql()).contains("org_code IN (:orgCodes)");
        assertThat(q.getRewrittenSql()).doesNotContain("is_deleted");
        assertThat(q.getParams()).containsKey("orgCodes");
    }

    @Test @DisplayName("collection_record (VIA_JOIN) 注入 INNER JOIN loan_account + la 机构过滤")
    void injectViaJoin() {
        SafeQuery q = guard.enforce(
            "SELECT record_id, loan_account FROM collection_record WHERE method = '电话'",
            scope("B001"));

        assertThat(q.getRewrittenSql())
            .contains("INNER JOIN loan_account")
            .contains("la.branch_code IN (:branchCodes)")
            .contains("is_deleted = 0");
    }

    @Test @DisplayName("超限 LIMIT 被 clamp 到上限")
    void clampsLimit() {
        SafeQuery q = guard.enforce(
            "SELECT loan_account FROM loan_account LIMIT 5000", scope("B001"));

        assertThat(q.getRewrittenSql()).contains("LIMIT 1000");
    }

    @Test @DisplayName("无权限（空 branchCodes）fail-closed 拒绝")
    void failClosedOnEmptyScope() {
        AiUserScope empty = scope();
        assertThatThrownBy(() -> guard.enforce("SELECT * FROM loan_account", empty))
            .isInstanceOf(SqlGuardException.class);
    }

    @Test @DisplayName("null 权限 = 不限（管理员），不注入机构过滤")
    void nullScopeMeansNoRestriction() {
        AiUserScope admin = new AiUserScope();
        admin.setAllowedBranchCodes(null);
        admin.setAllowedOrgCodes(null);

        SafeQuery q = guard.enforce("SELECT loan_account FROM loan_account", admin);

        assertThat(q.getRewrittenSql())
            .doesNotContain("branch_code IN")
            .doesNotContain("org_code IN")
            .contains("is_deleted = 0");
    }
}
