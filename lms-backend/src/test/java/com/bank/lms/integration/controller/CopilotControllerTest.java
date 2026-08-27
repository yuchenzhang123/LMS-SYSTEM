package com.bank.lms.integration.controller;

import com.bank.lms.config.AiQueryContext;
import com.bank.lms.dto.analysis.AiUserScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.bank.lms.common.TestConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller 集成测试 — 真实 Spring 上下文 + 真实 Service，需要测试数据库
 * 运行: mvn test -Dtest=CopilotControllerTest -Dspring.profiles.active=test -Dgroups=db-required
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CopilotController (集成)")
class CopilotControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void setUpContext() {
        // 注入用户上下文：拦截器依赖真实请求参数，这里直接构造以便端到端跑通
        AiUserScope scope = new AiUserScope();
        scope.setEhrNo(TEST_EHR_NO);
        scope.setOrgCode(TEST_ORG_CODE);
        scope.setUserRole(ROLE_ADMIN);
        scope.setAllowedOrgCodes(Collections.singletonList(TEST_ORG_CODE));
        scope.setAllowedBranchCodes(Arrays.asList(TEST_BRANCH_CODE, TEST_ORG_CODE));
        AiQueryContext.set(scope);
    }

    @AfterEach
    void clearContext() {
        AiQueryContext.clear();
    }

    @Test @DisplayName("每日简报 → 200（真实Service）")
    void briefingRealService() throws Exception {
        mockMvc.perform(post(API_AI_BRIEFING)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS));
    }

    @Test @DisplayName("AI问答 → 200（真实降级）")
    void chatRealService() throws Exception {
        mockMvc.perform(post(API_AI_CHAT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonMap("question", "测试"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS))
            .andExpect(jsonPath("$.data.answer").exists());
    }

    @Test @DisplayName("催收摘要 → 200")
    void summaryRealService() throws Exception {
        mockMvc.perform(post(API_AI_SUMMARY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonMap("loanAccount", TEST_LOAN_ACCOUNT))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS));
    }
}
