package com.bank.lms.service.embedding;

import java.util.List;

/**
 * 未启用 embedding 时的空实现：embed 返回 null，召回自然失效（退化为普通对话）。
 * 保证知识库 CRUD 在未配置向量模型时仍可用（文本落库，只是无法向量召回）。
 */
public class NoopEmbeddingClient implements EmbeddingClient {

    @Override
    public float[] embed(String text) {
        return null;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
