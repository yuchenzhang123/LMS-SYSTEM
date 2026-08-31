package com.bank.lms.unit;

import com.bank.lms.repository.AiQueryAuditLogRepository;
import com.bank.lms.service.knowledge.KnowledgeBaseService;
import com.bank.lms.service.knowledge.KnowledgeVectorStore;
import com.bank.lms.service.llm.LlmClient;
import com.bank.lms.service.nl2sql.Nl2SqlPlan;
import com.bank.lms.service.nl2sql.Nl2SqlService;
import com.bank.lms.service.nl2sql.SchemaRegistry;
import com.bank.lms.service.nl2sql.SqlSafetyGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NL2SQL 规划阶段 RAG 召回增强 — 纯 JUnit + Mockito，不依赖 Spring 上下文、不依赖真实 LLM/embedding。
 *
 * 验证两步式规划（选表 → 生成 SQL）中：
 *   1. 第二步（生成 SQL）的 planning prompt 拼接「参考知识」上下文（含分类标签与正文）；
 *   2. 无召回时第二步 prompt 不注入参考知识（降级为纯 schema 字典）；
 *   3. LLM 不可用时 plan 返回 null（调用方走 directChat 兜底），不抛异常。
 */
@DisplayName("NL2SQL 规划 RAG 召回增强")
class Nl2SqlRagContextTest {

    private LlmClient llmClient;
    private SchemaRegistry schemaRegistry;
    private SqlSafetyGuard sqlSafetyGuard;
    private org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate mainJdbcTemplate;
    private AiQueryAuditLogRepository auditLogRepository;
    private KnowledgeBaseService knowledgeBaseService;
    private Nl2SqlService service;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        schemaRegistry = mock(SchemaRegistry.class);
        sqlSafetyGuard = mock(SqlSafetyGuard.class);
        mainJdbcTemplate = mock(org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate.class);
        auditLogRepository = mock(AiQueryAuditLogRepository.class);
        knowledgeBaseService = mock(KnowledgeBaseService.class);
        // 两步式：选表 prompt 用 buildTableSummary，生成 SQL prompt 用 buildPrompt(tables)
        when(schemaRegistry.buildTableSummary()).thenReturn("测试表清单");
        when(schemaRegistry.buildPrompt(nullable(List.class))).thenReturn("测试表结构");
        service = new Nl2SqlService(llmClient, schemaRegistry, sqlSafetyGuard,
            mainJdbcTemplate, new ObjectMapper(), auditLogRepository, knowledgeBaseService);
    }

    private KnowledgeVectorStore.SearchHit hit(String title, String category, String content) {
        return new KnowledgeVectorStore.SearchHit(title, category, content, 0.9d);
    }

    /** 让两步式 LLM 调用依次返回：选表 JSON → 生成 SQL JSON */
    private void mockTwoStepLlm(String question) {
        when(llmClient.chatJson(anyString(), eq(question)))
            .thenReturn("{\"intent\":\"nl2sql\",\"tables\":[\"collection_record\"]}")
            .thenReturn("{\"intent\":\"nl2sql\",\"sql\":\"SELECT 1\"}");
    }

    @Test
    @DisplayName("规划 prompt 拼接召回知识（schema + sql-example）")
    void planIncludesRagContext() {
        when(knowledgeBaseService.search(anyString(), anyList())).thenReturn(Arrays.asList(
            hit("催收完成率口径", "schema", "result 取值：已完成/未完成"),
            hit("完成率SQL示例", "sql-example", "SELECT operator_name, COUNT(*) FROM collection_record")
        ));
        mockTwoStepLlm("哪个员工效率最高");

        Nl2SqlPlan plan = service.plan("哪个员工效率最高");

        assertNotNull(plan);
        assertEquals("nl2sql", plan.getIntent());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(llmClient, times(2)).chatJson(captor.capture(), eq("哪个员工效率最高"));
        // 第二步（生成 SQL）的 system prompt 应含召回知识
        String systemPrompt = captor.getAllValues().get(1);
        assertTrue(systemPrompt.contains("参考知识"), "prompt 应含参考知识引导语");
        assertTrue(systemPrompt.contains("催收完成率口径"), "prompt 应含召回标题");
        assertTrue(systemPrompt.contains("schema"), "prompt 应展示分类标签 schema");
        assertTrue(systemPrompt.contains("sql-example"), "prompt 应展示分类标签 sql-example");
        assertTrue(systemPrompt.contains("result 取值：已完成/未完成"), "prompt 应含召回正文");
    }

    @Test
    @DisplayName("无召回时规划 prompt 不注入参考知识（降级为原 schema）")
    void planWithoutRagContext() {
        when(knowledgeBaseService.search(anyString(), anyList()))
            .thenReturn(Collections.<KnowledgeVectorStore.SearchHit>emptyList());
        mockTwoStepLlm("哪个员工效率最高");

        service.plan("哪个员工效率最高");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(llmClient, times(2)).chatJson(captor.capture(), eq("哪个员工效率最高"));
        assertFalse(captor.getAllValues().get(1).contains("参考知识"), "空召回时不应注入参考知识");
    }

    @Test
    @DisplayName("选表阶段识别为闲聊时直接返回 chat，不进入第二步")
    void planReturnsChatWithoutSecondStep() {
        when(llmClient.chatJson(anyString(), eq("你好")))
            .thenReturn("{\"intent\":\"chat\"}");

        Nl2SqlPlan plan = service.plan("你好");

        assertNotNull(plan);
        assertEquals("chat", plan.getIntent());
        // 只调用了一次 LLM（选表阶段即短路），未进入生成 SQL 步骤
        verify(llmClient, times(1)).chatJson(anyString(), eq("你好"));
    }

    @Test
    @DisplayName("LLM 不可用时 plan 返回 null（不抛异常）")
    void planReturnsNullWhenLlmUnavailable() {
        when(knowledgeBaseService.search(anyString(), anyList()))
            .thenReturn(Collections.<KnowledgeVectorStore.SearchHit>emptyList());
        when(llmClient.chatJson(anyString(), anyString())).thenReturn(null);

        assertNull(service.plan("哪个员工效率最高"));
    }
}
