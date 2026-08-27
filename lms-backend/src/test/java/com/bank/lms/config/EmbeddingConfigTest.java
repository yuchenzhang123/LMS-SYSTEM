package com.bank.lms.config;

import com.bank.lms.service.embedding.EmbeddingClient;
import com.bank.lms.service.embedding.NoopEmbeddingClient;
import com.bank.lms.service.embedding.OpenAiEmbeddingClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Embedding 装配逻辑单测 — 不依赖 Spring 上下文，直接 new {@link EmbeddingConfig} 调用 bean 方法。
 *
 * 验证 lms.embedding.* 配置 → 客户端实例化规则：
 * 未启用或缺 api-url / api-key 任一 → NoopEmbeddingClient（降级，CRUD 仍可用）；
 * 启用且配置完整 → OpenAiEmbeddingClient。
 */
@DisplayName("Embedding装配")
class EmbeddingConfigTest {

    /** 按给定配置构造客户端实例 */
    private EmbeddingClient build(boolean enabled, String url, String key) {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setEnabled(enabled);
        props.setApiUrl(url);
        props.setApiKey(key);
        return new EmbeddingConfig(props).embeddingClient();
    }

    @Test
    @DisplayName("未启用 → Noop 降级")
    void disabled() {
        assertTrue(build(false, "http://x", "sk-x") instanceof NoopEmbeddingClient);
    }

    @Test
    @DisplayName("启用但缺 api-url → Noop 降级")
    void enabledWithoutUrl() {
        assertTrue(build(true, "", "sk-x") instanceof NoopEmbeddingClient);
    }

    @Test
    @DisplayName("启用但缺 api-key → Noop 降级")
    void enabledWithoutKey() {
        assertTrue(build(true, "http://x", "") instanceof NoopEmbeddingClient);
    }

    @Test
    @DisplayName("启用且配置完整 → OpenAi")
    void enabledWithFullConfig() {
        assertTrue(build(true, "http://x/v1/embeddings", "sk-x") instanceof OpenAiEmbeddingClient);
    }
}
