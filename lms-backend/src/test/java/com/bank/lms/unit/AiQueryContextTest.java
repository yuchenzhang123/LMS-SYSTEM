package com.bank.lms.unit;

import com.bank.lms.config.AiQueryContext;
import com.bank.lms.dto.analysis.AiUserScope;
import org.junit.jupiter.api.*;

import java.util.Arrays;

import static com.bank.lms.common.TestConstants.*;
import static org.assertj.core.api.Assertions.*;

/**
 * 单元测试 — AI查询上下文 ThreadLocal 隔离
 */
@DisplayName("AI查询上下文 (单元测试)")
class AiQueryContextTest {

    @AfterEach
    void tearDown() {
        AiQueryContext.clear();
    }

    @Test @DisplayName("设置和获取用户范围")
    void setAndGet() {
        AiUserScope scope = new AiUserScope();
        scope.setEhrNo(TEST_EHR_NO);
        scope.setOrgCode(TEST_ORG_CODE);
        scope.setUserRole(ROLE_MANAGER);
        scope.setAllowedBranchCodes(Arrays.asList(TEST_ORG_CODE, TEST_BRANCH_CODE));

        AiQueryContext.set(scope);

        assertThat(AiQueryContext.get())
            .isNotNull()
            .satisfies(s -> {
                assertThat(s.getEhrNo()).isEqualTo(TEST_EHR_NO);
                assertThat(s.getOrgCode()).isEqualTo(TEST_ORG_CODE);
                assertThat(s.getUserRole()).isEqualTo(ROLE_MANAGER);
                assertThat(s.getAllowedBranchCodes()).hasSize(2);
            });
    }

    @Test @DisplayName("未设置时返回null")
    void getWithoutSetReturnsNull() {
        assertThat(AiQueryContext.get()).isNull();
    }

    @Test @DisplayName("清除后返回null")
    void clearRemovesScope() {
        AiQueryContext.set(new AiUserScope());
        AiQueryContext.clear();
        assertThat(AiQueryContext.get()).isNull();
    }

    @Test @DisplayName("两次设置不互相干扰")
    void sequentialSetDoesNotLeak() {
        AiUserScope first = new AiUserScope();
        first.setEhrNo("first");
        AiQueryContext.set(first);
        assertThat(AiQueryContext.get().getEhrNo()).isEqualTo("first");

        AiQueryContext.clear();

        AiUserScope second = new AiUserScope();
        second.setEhrNo("second");
        AiQueryContext.set(second);
        assertThat(AiQueryContext.get().getEhrNo()).isEqualTo("second");
    }
}
