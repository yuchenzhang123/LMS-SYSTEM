package com.bank.lms.config;

import com.bank.lms.service.embedding.EmbeddingClient;
import com.bank.lms.service.embedding.NoopEmbeddingClient;
import com.bank.lms.service.embedding.OpenAiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Embedding 客户端配置：按 {@code lms.embedding.*} 装配。
 * 未启用时使用 {@link NoopEmbeddingClient}（知识库 CRUD 仍可用，仅向量召回失效）。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EmbeddingConfig {

    private final EmbeddingProperties properties;

    @Bean
    public EmbeddingClient embeddingClient() {
        if (properties.isEnabled() && valid(properties.getApiUrl(), properties.getApiKey())) {
            log.info("Embedding 客户端初始化完成: model={}", properties.getModel());
            return new OpenAiEmbeddingClient(new RestTemplate(), properties.getApiUrl(),
                properties.getApiKey(), properties.getModel());
        }
        log.info("Embedding 未启用，使用 NoopEmbeddingClient（知识召回失效，仅落库文本）");
        return new NoopEmbeddingClient();
    }

    private boolean valid(String url, String key) {
        return url != null && !url.trim().isEmpty() && key != null && !key.trim().isEmpty();
    }
}
