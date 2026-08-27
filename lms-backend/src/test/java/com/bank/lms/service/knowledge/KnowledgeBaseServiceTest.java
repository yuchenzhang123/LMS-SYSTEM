package com.bank.lms.service.knowledge;

import com.bank.lms.config.EmbeddingProperties;
import com.bank.lms.entity.KnowledgeBase;
import com.bank.lms.repository.KnowledgeBaseRepository;
import com.bank.lms.service.embedding.EmbeddingClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识库 CRUD 编排单测 — 纯 JUnit + Mockito，不依赖 Spring 上下文。
 *
 * 直接 new {@link KnowledgeBaseService} 注入 mock 依赖，验证
 * 「切块 → 落库 → 刷新索引」的编排与参数校验。
 * TextChunker 为静态工具类，测试中走真实实现。
 */
@DisplayName("知识库CRUD编排")
class KnowledgeBaseServiceTest {

    private KnowledgeBaseRepository repository;
    private EmbeddingClient embeddingClient;
    private EmbeddingProperties properties;
    private KnowledgeVectorStore vectorStore;
    private KnowledgeBaseService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeBaseRepository.class);
        embeddingClient = mock(EmbeddingClient.class);
        properties = new EmbeddingProperties();
        properties.setChunkSize(50);
        properties.setChunkOverlap(0);
        vectorStore = mock(KnowledgeVectorStore.class);
        service = new KnowledgeBaseService(repository, embeddingClient, properties, vectorStore, new ObjectMapper());
        // 默认场景：embedding 未启用，仅落库文本、向量为空
        when(embeddingClient.isAvailable()).thenReturn(false);
    }

    @Test
    @DisplayName("新增文本：短文本切 1 块落库并刷新索引")
    void addTextShort() {
        service.addText("催收话术", "运营", "客户逾期后第一时间电话联系。");

        ArgumentCaptor<KnowledgeBase> captor = ArgumentCaptor.forClass(KnowledgeBase.class);
        verify(repository, times(1)).save(captor.capture());
        KnowledgeBase saved = captor.getValue();
        assertEquals("催收话术", saved.getTitle());
        assertEquals("运营", saved.getCategory());
        assertEquals(0, saved.getChunkIndex());
        assertEquals(1, saved.getChunkTotal());
        assertNull(saved.getEmbedding(), "未启用 embedding 时向量应为空");
        verify(vectorStore, times(1)).refresh();
    }

    @Test
    @DisplayName("新增文本：长文本切多块，每块序号递增且总块数一致")
    void addTextMultiChunk() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append("第").append(i).append("句话用于测试切块逻辑。");
        }
        service.addText("长知识", null, sb.toString());

        ArgumentCaptor<KnowledgeBase> captor = ArgumentCaptor.forClass(KnowledgeBase.class);
        verify(repository, atLeast(2)).save(captor.capture());
        List<KnowledgeBase> savedList = captor.getAllValues();
        assertTrue(savedList.size() > 1, "长文本应切为多块");
        for (int i = 0; i < savedList.size(); i++) {
            assertEquals(i, savedList.get(i).getChunkIndex(), "第 " + i + " 块序号应递增");
            assertEquals(savedList.size(), savedList.get(i).getChunkTotal());
        }
        verify(vectorStore, times(1)).refresh();
    }

    @Test
    @DisplayName("编辑：先软删旧切块再重新落库")
    void update() {
        service.update("催收话术", "运营", "更新后的内容。");
        verify(repository).softDeleteByTitle("催收话术");
        verify(repository, atLeastOnce()).save(any(KnowledgeBase.class));
        verify(vectorStore, times(1)).refresh();
    }

    @Test
    @DisplayName("删除：软删并刷新索引")
    void delete() {
        service.delete("催收话术");
        verify(repository).softDeleteByTitle("催收话术");
        verify(vectorStore, times(1)).refresh();
    }

    @Test
    @DisplayName("参数校验：标题/内容为空抛异常")
    void invalidParams() {
        assertThrows(IllegalArgumentException.class, () -> service.addText("", "c", "内容"));
        assertThrows(IllegalArgumentException.class, () -> service.addText("标题", "c", "   "));
        assertThrows(IllegalArgumentException.class, () -> service.delete("   "));
    }

    @Test
    @DisplayName("embedding 启用时对每个切块向量化并落库")
    void addTextWithEmbedding() throws Exception {
        when(embeddingClient.isAvailable()).thenReturn(true);
        // 返回与输入文本等长的向量列表（模拟 /embeddings batch）
        when(embeddingClient.embedBatch(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            List<float[]> vecs = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                vecs.add(new float[]{i * 0.1f, (i + 1) * 0.1f});
            }
            return vecs;
        });

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("长句").append(i).append("用于制造多个切块。");
        }
        service.addText("带向量知识", null, sb.toString());

        ArgumentCaptor<KnowledgeBase> captor = ArgumentCaptor.forClass(KnowledgeBase.class);
        verify(repository, atLeast(2)).save(captor.capture());
        for (KnowledgeBase kb : captor.getAllValues()) {
            assertNotNull(kb.getEmbedding(), "启用 embedding 后每个切块向量不应为空");
        }
    }
}
