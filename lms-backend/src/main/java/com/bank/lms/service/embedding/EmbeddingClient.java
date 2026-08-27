package com.bank.lms.service.embedding;

import java.util.List;

/**
 * Embedding（文本向量化）客户端接口。
 *
 * 与 {@link com.bank.lms.service.llm.LlmClient} 解耦：embedding 模型与对话 LLM
 * 可能来自不同服务/模型，因此单独定义客户端，由 {@link com.bank.lms.config.EmbeddingConfig}
 * 按 {@code lms.embedding.*} 配置装配。
 */
public interface EmbeddingClient {

    /**
     * 单条文本向量化。
     * @return 向量（维度由模型决定），不可用时返回 null
     */
    float[] embed(String text);

    /**
     * 批量向量化（一次请求多条，减少网络往返）。
     * @return 与输入顺序一致的向量列表，不可用时返回 null
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 是否可用
     */
    boolean isAvailable();
}
