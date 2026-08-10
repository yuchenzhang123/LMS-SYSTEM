package com.bank.lms.service.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 LLM 客户端（通义千问等国内模型 API 也兼容此格式）
 */
@Slf4j
public class OpenAiLlmClient implements LlmClient {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final int timeout;

    public OpenAiLlmClient(RestTemplate restTemplate,
                           @Value("${lms.llm.api-url}") String apiUrl,
                           @Value("${lms.llm.api-key}") String apiKey,
                           @Value("${lms.llm.model:gpt-4o-mini}") String model,
                           @Value("${lms.llm.timeout:30000}") int timeout) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = timeout;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        if (!isAvailable()) return null;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.3,
                "max_tokens", 500
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    return (String) message.get("content");
                }
            }
            log.warn("LLM 返回格式异常: {}", response.getBody());
            return null;
        } catch (Exception e) {
            log.error("LLM 调用失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return apiUrl != null && !apiUrl.isEmpty() && apiKey != null && !apiKey.isEmpty();
    }
}
