package com.bank.lms.service.knowledge;

import com.bank.lms.config.EmbeddingProperties;
import com.bank.lms.entity.KnowledgeBase;
import com.bank.lms.repository.KnowledgeBaseRepository;
import com.bank.lms.service.embedding.EmbeddingClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 知识库向量索引（应用层内存计算）。
 *
 * 为什么不用独立向量库（Milvus/Qdrant/pgvector）：
 *   知识库规模为几百~几千条（话术/政策/FAQ），每条 1536 维，全量内存暴力余弦相似度仅毫秒级；
 *   引入独立向量库需额外部署服务且较新 SDK 要求 Java 11+，与项目 Java 1.8 冲突，收益不成比例。
 *   索引接口已抽象（search/refresh），未来知识量暴涨可平滑替换为独立向量库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeVectorStore {

    private final KnowledgeBaseRepository repository;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingProperties properties;
    private final ObjectMapper objectMapper;

    /** 内存向量索引（启动加载，增删改后 refresh 重建） */
    private volatile List<ChunkEntry> index = new ArrayList<>();

    @PostConstruct
    public void init() {
        // 启动时表可能尚未初始化，失败不阻断启动，后续首次写操作成功后会 refresh 重建
        try {
            refresh();
        } catch (Exception e) {
            log.warn("知识库向量索引启动加载失败（可能表未初始化）: {}", e.getMessage());
        }
    }

    /** 全量重建内存索引（增删改后调用） */
    public synchronized void refresh() {
        List<KnowledgeBase> all = repository.findAll();
        List<ChunkEntry> loaded = new ArrayList<>();
        for (KnowledgeBase kb : all) {
            float[] vec = deserialize(kb.getEmbedding());
            if (vec != null) {
                loaded.add(new ChunkEntry(kb.getId(), kb.getTitle(), kb.getCategory(), kb.getContent(), vec));
            }
        }
        this.index = loaded;
        log.info("知识库向量索引刷新完成: chunk数={}", loaded.size());
    }

    /** 向量召回（不限分类，供 chat 分支使用） */
    public List<SearchHit> search(String query) {
        return search(query, null);
    }

    /**
     * 向量召回：query 向量化后在内存索引上做余弦相似度 Top-K。
     *
     * @param categories 限定分类集合；null 表示不限分类
     * @return 命中片段列表（按相似度降序）；embedding 不可用或索引为空时返回空列表
     */
    public List<SearchHit> search(String query, Set<String> categories) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (!embeddingClient.isAvailable() || index.isEmpty()) {
            return Collections.emptyList();
        }
        float[] queryVec = embeddingClient.embed(query.trim());
        if (queryVec == null) {
            return Collections.emptyList();
        }

        List<SearchHit> hits = new ArrayList<>();
        for (ChunkEntry entry : index) {
            // 分类过滤：categories 非空时，entry.category 不在集合内则跳过（null 分类不参与）
            if (categories != null && (entry.category == null || !categories.contains(entry.category))) {
                continue;
            }
            double sim = cosine(queryVec, entry.vec);
            if (sim >= properties.getMinScore()) {
                hits.add(new SearchHit(entry.title, entry.category, entry.content, sim));
            }
        }
        hits.sort((a, b) -> Double.compare(b.score, a.score));
        int k = Math.min(properties.getTopK(), hits.size());
        return hits.subList(0, k);
    }

    /** 余弦相似度 */
    private double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0;
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private float[] deserialize(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (Exception e) {
            log.warn("向量反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /** 内存索引条目 */
    @Data
    @RequiredArgsConstructor
    static class ChunkEntry {
        private final Long id;
        private final String title;
        private final String category;
        private final String content;
        private final float[] vec;
    }

    /** 召回命中结果 */
    @Data
    @RequiredArgsConstructor
    public static class SearchHit {
        private final String title;
        private final String category;
        private final String content;
        private final double score;
    }
}
