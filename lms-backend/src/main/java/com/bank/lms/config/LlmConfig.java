package com.bank.lms.config;

import com.bank.lms.service.llm.LlmClient;
import com.bank.lms.service.llm.NoopLlmClient;
import com.bank.lms.service.llm.OpenAiLlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * LLM 客户端配置
 */
@Slf4j
@Configuration
public class LlmConfig {

    @Value("${lms.llm.enabled:false}")
    private boolean llmEnabled;

    @Value("${lms.llm.api-url:}")
    private String apiUrl;

    @Value("${lms.llm.api-key:}")
    private String apiKey;

    @Value("${lms.llm.model:gpt-4o-mini}")
    private String model;

    @Value("${lms.llm.timeout:30000}")
    private int timeout;

    @Bean
    public LlmClient llmClient() {
        if (!llmEnabled || apiKey.isEmpty()) {
            log.info("LLM 未启用，使用 NoopLlmClient（降级到规则模板）");
            return new NoopLlmClient();
        }
        log.info("LLM 客户端初始化: model={}, url={}", model, apiUrl);
        return new OpenAiLlmClient(new RestTemplate(), apiUrl, apiKey, model, timeout);
    }
}
