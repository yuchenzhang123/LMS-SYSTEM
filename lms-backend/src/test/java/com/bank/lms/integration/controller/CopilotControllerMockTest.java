package com.bank.lms.integration.controller;

import com.bank.lms.common.GlobalExceptionHandler;
import com.bank.lms.controller.CopilotController;
import com.bank.lms.service.analysis.CopilotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.bank.lms.common.TestConstants.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller 单元测试 — standalone MockMvc，零 Spring 上下文，无需数据库
 * 本地 mvn test 直接跑
 */
@DisplayName("CopilotController (Mock)")
class CopilotControllerMockTest {

    private MockMvc mockMvc;
    private CopilotService copilotService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        copilotService = mock(CopilotService.class);

        mockMvc = MockMvcBuilders
            .standaloneSetup(new CopilotController(copilotService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();

        Map<String, Object> mockAnswer = new HashMap<>();
        mockAnswer.put("answer", "测试回答内容");
        mockAnswer.put("capability", "ORG_RANKING");
        mockAnswer.put("data", java.util.Collections.emptyList());
        when(copilotService.ask(anyString())).thenReturn(mockAnswer);

        Map<String, Object> mockBriefing = new HashMap<>();
        mockBriefing.put("briefing", "今日简报测试内容");
        when(copilotService.dailyBriefing()).thenReturn(mockBriefing);

        Map<String, Object> mockSummary = new HashMap<>();
        mockSummary.put("summary", "催收摘要测试内容");
        mockSummary.put("totalRecords", 5L);
        when(copilotService.collectionSummary(anyString())).thenReturn(mockSummary);
    }

    // ==================== /ai/chat ====================

    @Test @DisplayName("正常问答 → code=0 + answer")
    void chatSuccess() throws Exception {
        mockMvc.perform(post(API_AI_CHAT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonMap("question", "逾期趋势如何"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS))
            .andExpect(jsonPath("$.data.answer").value("测试回答内容"));
    }

    @Test @DisplayName("空问题 → 400")
    void chatEmptyQuestion() throws Exception {
        mockMvc.perform(post(API_AI_CHAT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonMap("question", ""))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_PARAM_ERROR));
    }

    @Test @DisplayName("缺失question → 400")
    void chatMissingQuestion() throws Exception {
        mockMvc.perform(post(API_AI_CHAT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_PARAM_ERROR));
    }

    @Test @DisplayName("超长问题 → 正常")
    void chatLongQuestion() throws Exception {
        StringBuilder sb = new StringBuilder(); for (int i = 0; i < 500; i++) sb.append("test"); String longQ = sb.toString();
        mockMvc.perform(post(API_AI_CHAT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonMap("question", longQ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS));
    }

    // ==================== /ai/briefing ====================

    @Test @DisplayName("每日简报 → code=0")
    void briefingSuccess() throws Exception {
        mockMvc.perform(post(API_AI_BRIEFING)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS))
            .andExpect(jsonPath("$.data.briefing").value("今日简报测试内容"));
    }

    // ==================== /ai/summary ====================

    @Test @DisplayName("催收摘要缺少loanAccount → 400")
    void summaryMissingLoanAccount() throws Exception {
        mockMvc.perform(post(API_AI_SUMMARY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_PARAM_ERROR));
    }

    @Test @DisplayName("催收摘要 → code=0")
    void summarySuccess() throws Exception {
        mockMvc.perform(post(API_AI_SUMMARY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonMap("loanAccount", TEST_LOAN_ACCOUNT))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS))
            .andExpect(jsonPath("$.data.summary").value("催收摘要测试内容"))
            .andExpect(jsonPath("$.data.totalRecords").value(5));
    }

    // ==================== 异常 ====================

    @Test @DisplayName("Service抛异常 → 系统异常")
    void serviceThrowsException() throws Exception {
        when(copilotService.ask(anyString())).thenThrow(new RuntimeException("模拟异常"));
        mockMvc.perform(post(API_AI_CHAT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonMap("question", "测试"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SYSTEM_ERROR));
    }

    @Test @DisplayName("JSON格式错误 → 系统异常")
    void malformedJson() throws Exception {
        mockMvc.perform(post(API_AI_CHAT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("not valid {{{"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SYSTEM_ERROR));
    }
}
