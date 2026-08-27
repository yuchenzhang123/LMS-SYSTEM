package com.bank.lms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding（向量化）配置属性，独立于 {@link LlmProperties}（lms.llm.*）。
 *
 * 向量模型单独 yml 配置：知识库 RAG 召回用的 embedding 模型与对话 LLM 解耦，
 * 可分别指向不同服务/模型，互不影响。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "lms.embedding")
public class EmbeddingProperties {

    /** 向量化总开关，默认关闭。启用需同时配置 api-key */
    private boolean enabled = false;

    /** OpenAI 兼容 /embeddings 接口地址 */
    private String apiUrl = "";

    private String apiKey = "";

    /** embedding 模型名（如 text-embedding-3-small / bge-m3 / qwen3-embedding） */
    private String model = "text-embedding-3-small";

    /** 召回 Top-K，默认 5 */
    private int topK = 5;

    /** 相似度阈值（余弦相似度），低于该值不召回，默认 0（不过滤） */
    private double minScore = 0.0;

    /** 文本切块字符数（中文），默认 800 */
    private int chunkSize = 800;

    /** 切块重叠字符数，默认 100 */
    private int chunkOverlap = 100;
}
