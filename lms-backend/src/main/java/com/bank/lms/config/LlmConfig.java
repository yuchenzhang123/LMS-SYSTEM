package com.bank.lms.config;

import com.bank.lms.service.llm.LlmClient;
import com.bank.lms.service.llm.NoopLlmClient;
import com.bank.lms.service.llm.OpenAiLlmClient;
import com.bank.lms.service.llm.RoutingLlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 客户端配置：支持单 provider 与多 provider 主备。
 * 配置见 {@link LlmProperties}（lms.llm.*）。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LlmConfig {

    private final LlmProperties properties;

    @Bean
    public LlmClient llmClient() {
        List<LlmClient> providers = buildProviders();
        if (providers.isEmpty()) {
            log.info("LLM 未启用，使用 NoopLlmClient（降级到规则模板）");
            return new NoopLlmClient();
        }
        if (providers.size() == 1) {
            log.info("LLM 单 provider 初始化完成: model={}", properties.getModel());
            return providers.get(0);
        }
        log.info("LLM 多 provider 路由初始化完成: provider数={}", providers.size());
        return new RoutingLlmClient(providers);
    }

    private List<LlmClient> buildProviders() {
        List<LlmClient> result = new ArrayList<>();
        if (!properties.isEnabled()) {
            return result;
        }
        // 优先使用 providers[] 列表（多 provider 主备）
        if (properties.getProviders() != null && !properties.getProviders().isEmpty()) {
            for (LlmProperties.Provider p : properties.getProviders()) {
                if (valid(p.getApiUrl(), p.getApiKey())) {
                    result.add(new OpenAiLlmClient(new RestTemplate(), p.getApiUrl(), p.getApiKey(),
                        p.getModel(), p.getMaxTokens(), p.isEnableThinking(), p.getJsonMode()));
                }
            }
        } else if (valid(properties.getApiUrl(), properties.getApiKey())) {
            // 向后兼容：单 provider 顶层字段
            result.add(new OpenAiLlmClient(new RestTemplate(), properties.getApiUrl(), properties.getApiKey(),
                properties.getModel(), properties.getMaxTokens(), properties.isEnableThinking(), properties.getJsonMode()));
        }
        return result;
    }

    private boolean valid(String url, String key) {
        return url != null && !url.trim().isEmpty() && key != null && !key.trim().isEmpty();
    }
}
