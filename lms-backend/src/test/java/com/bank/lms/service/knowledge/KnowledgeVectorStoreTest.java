package com.bank.lms.service.knowledge;

import com.bank.lms.config.EmbeddingProperties;
import com.bank.lms.entity.KnowledgeBase;
import com.bank.lms.repository.KnowledgeBaseRepository;
import com.bank.lms.service.embedding.EmbeddingClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 知识库向量召回单测 — 纯 JUnit + Mockito，不依赖 Spring 上下文。
 *
 * 直接 new {@link KnowledgeVectorStore} 并注入 mock 依赖，
 * 通过公开的 refresh()/search() 验证余弦相似度召回行为
 * （cosine 为私有方法，行为通过 search 的结果序/过滤断言）。
 */
@DisplayName("知识库向量召回")
class KnowledgeVectorStoreTest {

    private KnowledgeBaseRepository repository;
    private EmbeddingClient embeddingClient;
    private EmbeddingProperties properties;
    private KnowledgeVectorStore store;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeBaseRepository.class);
        embeddingClient = mock(EmbeddingClient.class);
        properties = new EmbeddingProperties();
        store = new KnowledgeVectorStore(repository, embeddingClient, properties, objectMapper);
    }

    /** 构造一条带指定向量的知识块（embedding 序列化为 JSON 字符串） */
    private KnowledgeBase chunk(long id, String title, String content, float[] vec) throws Exception {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setTitle(title);
        kb.setContent(content);
        kb.setEmbedding(objectMapper.writeValueAsString(vec));
        return kb;
    }

    @Test
    @DisplayName("query 为空返回空列表")
    void searchNullQuery() {
        assertTrue(store.search(null).isEmpty());
        assertTrue(store.search("   ").isEmpty());
    }

    @Test
    @DisplayName("embedding 未启用返回空列表")
    void searchWhenEmbeddingDisabled() {
        when(embeddingClient.isAvailable()).thenReturn(false);
        assertTrue(store.search("逾期政策").isEmpty());
    }

    @Test
    @DisplayName("索引为空（无已向量化切块）返回空列表")
    void searchWhenIndexEmpty() throws Exception {
        when(embeddingClient.isAvailable()).thenReturn(true);
        when(repository.findAll()).thenReturn(Collections.<KnowledgeBase>emptyList());
        store.refresh();
        assertTrue(store.search("逾期政策").isEmpty());
    }

    @Test
    @DisplayName("余弦相似度：相同向量召回并排第一，正交向量排后")
    void searchCosineRanking() throws Exception {
        when(embeddingClient.isAvailable()).thenReturn(true);
        when(embeddingClient.embed("逾期多久起诉")).thenReturn(new float[]{1.0f, 0.0f});
        when(repository.findAll()).thenReturn(Arrays.asList(
            chunk(1L, "逾期起诉时限", "相关切块", new float[]{1.0f, 0.0f}),  // sim=1
            chunk(2L, "无关知识", "无关切块", new float[]{0.0f, 1.0f})     // sim=0
        ));
        store.refresh();

        List<KnowledgeVectorStore.SearchHit> hits = store.search("逾期多久起诉");
        assertEquals(2, hits.size());
        assertEquals("逾期起诉时限", hits.get(0).getTitle(), "相关向量应排第一");
        assertTrue(hits.get(0).getScore() > hits.get(1).getScore());
    }

    @Test
    @DisplayName("minScore 阈值过滤低相似度结果")
    void searchMinScoreFilter() throws Exception {
        properties.setMinScore(0.5);
        when(embeddingClient.isAvailable()).thenReturn(true);
        when(embeddingClient.embed("逾期多久起诉")).thenReturn(new float[]{1.0f, 0.0f});
        when(repository.findAll()).thenReturn(Arrays.asList(
            chunk(1L, "逾期起诉时限", "相关切块", new float[]{1.0f, 0.0f}),  // sim=1
            chunk(2L, "无关知识", "无关切块", new float[]{0.0f, 1.0f})      // sim=0，低于阈值被过滤
        ));
        store.refresh();

        List<KnowledgeVectorStore.SearchHit> hits = store.search("逾期多久起诉");
        assertEquals(1, hits.size());
        assertEquals("逾期起诉时限", hits.get(0).getTitle());
    }

    @Test
    @DisplayName("Top-K 截断召回数量")
    void searchTopK() throws Exception {
        properties.setTopK(1);
        when(embeddingClient.isAvailable()).thenReturn(true);
        when(embeddingClient.embed("逾期多久起诉")).thenReturn(new float[]{1.0f, 0.0f});
        when(repository.findAll()).thenReturn(Arrays.asList(
            chunk(1L, "a", "切块", new float[]{1.0f, 0.0f}),
            chunk(2L, "b", "切块", new float[]{0.9f, 0.1f}),
            chunk(3L, "c", "切块", new float[]{0.0f, 1.0f})
        ));
        store.refresh();

        List<KnowledgeVectorStore.SearchHit> hits = store.search("逾期多久起诉");
        assertEquals(1, hits.size());
        assertEquals("a", hits.get(0).getTitle(), "TopK=1 时应召回相似度最高的块");
    }
}
