package com.bank.lms.integration.controller;

import com.bank.lms.common.GlobalExceptionHandler;
import com.bank.lms.common.Result;
import com.bank.lms.config.BearerTokenFilter;
import com.bank.lms.controller.CopilotController;
import com.bank.lms.service.analysis.CopilotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.FilterChain;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.bank.lms.common.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 安全过滤 + 全局异常测试（standalone MockMvc + 直测过滤器，无需 Spring 上下文）
 */
@DisplayName("安全过滤 + 全局异常")
class SecurityFilterTest {

    // ==================== 一、BearerTokenFilter 直测 ====================

    @Nested
    @DisplayName("BearerTokenFilter Token 拦截")
    class TokenFilterTest {

        private BearerTokenFilter filter;

        @BeforeEach
        void setUp() {
            filter = new BearerTokenFilter();
        }

        @Test @DisplayName("auth禁用 → 直接放行")
        void authDisabledPassThrough() throws Exception {
            ReflectionTestUtils.setField(filter, "authEnabled", false);

            MockHttpServletRequest request = new MockHttpServletRequest("POST", API_AI_CHAT);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            assertThat(response.getStatus()).isNotEqualTo(401);
        }

        @Test @DisplayName("auth启用+白名单路径 → 放行")
        void whitelistPathPassThrough() throws Exception {
            ReflectionTestUtils.setField(filter, "authEnabled", true);

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/scheduler/sync-gbase");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }

        @Test @DisplayName("auth启用+无Authorization头 → 401")
        void missingTokenRejected() throws Exception {
            ReflectionTestUtils.setField(filter, "authEnabled", true);

            MockHttpServletRequest request = new MockHttpServletRequest("POST", API_AI_CHAT);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(401);
            verify(chain, never()).doFilter(any(), any());

            // 校验返回体是统一错误格式
            Result<?> body = new ObjectMapper().readValue(response.getContentAsString(), Result.class);
            assertThat(body.getCode()).isEqualTo(CODE_UNAUTHORIZED);
        }

        @Test @DisplayName("auth启用+非Bearer格式头 → 401")
        void invalidHeaderRejected() throws Exception {
            ReflectionTestUtils.setField(filter, "authEnabled", true);

            MockHttpServletRequest request = new MockHttpServletRequest("POST", API_AI_CHAT);
            request.addHeader("Authorization", "Basic abc123");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(401);
        }

        @Test @DisplayName("auth启用+token校验URL未配置 → 放行并告警")
        void noTokenCheckUrlPassThrough() throws Exception {
            ReflectionTestUtils.setField(filter, "authEnabled", true);
            ReflectionTestUtils.setField(filter, "tokenCheckUrl", "");

            MockHttpServletRequest request = new MockHttpServletRequest("POST", API_AI_CHAT);
            request.addHeader("Authorization", "Bearer any-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }
    }

    // ==================== 二、GlobalExceptionHandler 全局异常 ====================

    @Nested
    @DisplayName("GlobalExceptionHandler 全局异常")
    class GlobalExceptionTest {

        private MockMvc mockMvc;
        private CopilotService copilotService;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
            copilotService = mock(CopilotService.class);

            // standalone MockMvc + 真实的全局异常处理器
            mockMvc = MockMvcBuilders
                .standaloneSetup(new CopilotController(copilotService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        }

        @Test @DisplayName("RuntimeException → 统一错误格式")
        void runtimeException() throws Exception {
            when(copilotService.ask(anyString())).thenThrow(new RuntimeException("模拟异常"));

            String json = mockMvc.perform(post(API_AI_CHAT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Collections.singletonMap("question", "test"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

            Result<?> result = objectMapper.readValue(json, Result.class);
            assertThat(result.getCode()).isEqualTo(CODE_SYSTEM_ERROR);
        }

        @Test @DisplayName("非法JSON → 系统异常")
        void malformedJson() throws Exception {
            String json = mockMvc.perform(post(API_AI_CHAT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("not valid {{{"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

            Result<?> result = objectMapper.readValue(json, Result.class);
            assertThat(result.getCode()).isEqualTo(CODE_SYSTEM_ERROR);
        }

        @Test @DisplayName("空body → 不崩溃")
        void emptyBody() throws Exception {
            String json = mockMvc.perform(post(API_AI_CHAT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

            Result<?> result = objectMapper.readValue(json, Result.class);
            assertThat(result.getCode()).isNotNull();
        }

        @Test @DisplayName("SQL注入尝试 → 不崩溃正常返回")
        void sqlInjection() throws Exception {
            Map<String, Object> mockAnswer = new HashMap<>();
            mockAnswer.put("answer", "ok");
            mockAnswer.put("capability", "TEST");
            mockAnswer.put("data", Collections.emptyList());
            when(copilotService.ask(anyString())).thenReturn(mockAnswer);

            String json = mockMvc.perform(post(API_AI_CHAT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Collections.singletonMap("question", "'; DROP TABLE loan_account; --"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

            Result<?> result = objectMapper.readValue(json, Result.class);
            assertThat(result.getCode()).isEqualTo(CODE_SUCCESS);
        }

        @Test @DisplayName("超大请求体 → 不崩溃")
        void hugePayload() throws Exception {
            StringBuilder sb = new StringBuilder(); for (int i = 0; i < 10000; i++) sb.append("x"); String huge = "{\"question\":\"" + sb.toString() + "\"}";
            String json = mockMvc.perform(post(API_AI_CHAT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(huge))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

            Result<?> result = objectMapper.readValue(json, Result.class);
            assertThat(result.getCode()).isNotNull();
        }
    }
}
