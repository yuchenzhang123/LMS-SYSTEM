package com.bank.lms.integration.controller;

import com.bank.lms.common.GlobalExceptionHandler;
import com.bank.lms.controller.KnowledgeBaseController;
import com.bank.lms.service.knowledge.KnowledgeBaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bank.lms.common.TestConstants.CODE_PARAM_ERROR;
import static com.bank.lms.common.TestConstants.CODE_SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 知识库 Controller 单元测试 — standalone MockMvc，零 Spring 上下文，无需数据库
 * 本地 mvn test 直接跑
 */
@DisplayName("KnowledgeBaseController (Mock)")
class KnowledgeBaseControllerMockTest {

    private MockMvc mockMvc;
    private KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        knowledgeBaseService = mock(KnowledgeBaseService.class);

        mockMvc = MockMvcBuilders
            .standaloneSetup(new KnowledgeBaseController(knowledgeBaseService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    // ==================== 列表 ====================

    @Test @DisplayName("列表 → code=0 + 聚合数据")
    void listSuccess() throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("title", "催收话术");
        item.put("chunkCount", 3);
        list.add(item);
        when(knowledgeBaseService.list()).thenReturn(list);

        mockMvc.perform(get("/knowledge/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS))
            .andExpect(jsonPath("$.data[0].title").value("催收话术"))
            .andExpect(jsonPath("$.data[0].chunkCount").value(3));
    }

    // ==================== 查询原文 ====================

    @Test @DisplayName("查询某标题原文 → code=0")
    void getByTitleSuccess() throws Exception {
        // 路径变量用 ASCII，规避 MockMvc standalone 对中文 URL 的 ISO-8859-1 解码差异
        Map<String, Object> detail = new HashMap<>();
        detail.put("title", "KB_POLICY");
        detail.put("content", "客户逾期后及时电话联系。");
        when(knowledgeBaseService.getByTitle("KB_POLICY")).thenReturn(detail);

        mockMvc.perform(get("/knowledge/{title}", "KB_POLICY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS))
            .andExpect(jsonPath("$.data.content").value("客户逾期后及时电话联系。"));
    }

    // ==================== 新增 ====================

    @Test @DisplayName("新增文本 → code=0")
    void addSuccess() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("title", "催收话术");
        body.put("category", "运营");
        body.put("content", "客户逾期后及时电话联系。");

        mockMvc.perform(post("/knowledge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS))
            .andExpect(jsonPath("$.data").value("新增知识成功"));

        verify(knowledgeBaseService).addText("催收话术", "运营", "客户逾期后及时电话联系。");
    }

    @Test @DisplayName("Service 校验失败 → 400")
    void addInvalid() throws Exception {
        // 空 body 时 controller 传 null，any() 才能匹配（anyString 不匹配 null）
        doThrow(new IllegalArgumentException("标题不能为空"))
            .when(knowledgeBaseService).addText(any(), any(), any());

        mockMvc.perform(post("/knowledge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new HashMap<String, String>())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_PARAM_ERROR));
    }

    // ==================== 上传导入 ====================

    @Test @DisplayName("上传文件导入 → code=0")
    void importSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "政策.txt", "text/plain", "逾期政策正文".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/knowledge/import").file(file).param("category", "政策"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS))
            .andExpect(jsonPath("$.data").value("导入知识成功"));

        verify(knowledgeBaseService).importFile(any(), eq("政策"));
    }

    // ==================== 编辑 / 删除 ====================

    @Test @DisplayName("编辑 → code=0，category 可为空")
    void updateSuccess() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("content", "更新后的内容");

        mockMvc.perform(put("/knowledge/{title}", "KB_POLICY")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS))
            .andExpect(jsonPath("$.data").value("更新知识成功"));

        verify(knowledgeBaseService).update(eq("KB_POLICY"), isNull(), eq("更新后的内容"));
    }

    @Test @DisplayName("删除 → code=0")
    void deleteSuccess() throws Exception {
        mockMvc.perform(delete("/knowledge/{title}", "KB_POLICY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS))
            .andExpect(jsonPath("$.data").value("删除知识成功"));

        verify(knowledgeBaseService).delete("KB_POLICY");
    }
}
