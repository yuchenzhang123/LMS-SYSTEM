package com.bank.lms.service.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 LLM 客户端（通义千问等国内模型 API 也兼容此格式）。
 */
@Slf4j
public class OpenAiLlmClient implements LlmClient {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    /** 默认是否开启思考模式（作为 chat 两参重载的默认值，具体调用可覆盖） */
    private final boolean defaultEnableThinking;
    /** JSON 输出模式：prompt（仅 prompt 约束，最保守）| response_format（附加 response_format 参数） */
    private final String jsonMode;

    public OpenAiLlmClient(RestTemplate restTemplate,
                           @Value("${lms.llm.api-url}") String apiUrl,
                           @Value("${lms.llm.api-key}") String apiKey,
                           @Value("${lms.llm.model:gpt-4o-mini}") String model,
                           @Value("${lms.llm.max-tokens:4096}") int maxTokens,
                           @Value("${lms.llm.enable-thinking:true}") boolean defaultEnableThinking,
                           @Value("${lms.llm.json-mode:prompt}") String jsonMode) {
        // 必须设置超时：LLM 服务挂起时不能让业务线程无限阻塞
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 连接超时 5s
        factory.setReadTimeout(60000);     // 读超时 60s（思考模式推理耗时较长）
        restTemplate.setRequestFactory(factory);

        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.defaultEnableThinking = defaultEnableThinking;
        this.jsonMode = jsonMode == null || jsonMode.isEmpty() ? "prompt" : jsonMode;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, defaultEnableThinking);
    }

    @Override
    public String chat(String systemPrompt, String userMessage, boolean enableThinking) {
        return doChat(systemPrompt, userMessage, enableThinking, false);
    }

    @Override
    public String chatJson(String systemPrompt, String userMessage) {
        return doChat(systemPrompt, userMessage, false, true);
    }

    private String doChat(String systemPrompt, String userMessage, boolean enableThinking, boolean wantJson) {
        if (!isAvailable()) return null;

        long start = System.currentTimeMillis();
        log.debug("LLM调用开始: model={}, enableThinking={}, wantJson={}, url={}", model, enableThinking, wantJson, apiUrl);

        String content = doChatOnce(systemPrompt, userMessage, enableThinking, wantJson, start);
        // 思考模式 content 为空时，降级为无思考重试一次：
        // 内网模型可能忽略 enable_thinking=false 仍强制思考，思考过程耗尽 max_tokens 导致 content 为空
        if (content == null && enableThinking) {
            log.warn("思考模式未产出有效答案，降级为无思考重试一次");
            content = doChatOnce(systemPrompt, userMessage, false, wantJson, start);
        }
        return content;
    }

    /** 单次 LLM 调用（不重试），返回最终答案或 null */
    private String doChatOnce(String systemPrompt, String userMessage, boolean enableThinking, boolean wantJson, long start) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = buildBody(systemPrompt, userMessage, enableThinking, wantJson);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            log.debug("LLM HTTP响应: status={}, 耗时={}ms", response.getStatusCode(), System.currentTimeMillis() - start);

            return extractContent(response, start);
        } catch (Exception e) {
            log.error("LLM 调用失败: url={}, 耗时={}ms, 异常={}", apiUrl, System.currentTimeMillis() - start, e.toString());
            return null;
        }
    }

    private Map<String, Object> buildBody(String systemPrompt, String userMessage, boolean enableThinking, boolean wantJson) {
        Map<String, Object> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(sysMsg);
        messages.add(userMsg);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("max_tokens", maxTokens);
        // 按调用场景控制思考模式：归因分析开，简单直出关
        body.put("enable_thinking", enableThinking);

        // JSON 模式：仅在显式配置 response_format 时附加（不假定内网 32B 支持）
        if (wantJson && "response_format".equalsIgnoreCase(jsonMode)) {
            Map<String, Object> fmt = new HashMap<>();
            fmt.put("type", "json_object");
            body.put("response_format", fmt);
        }
        return body;
    }

    private String extractContent(ResponseEntity<Map> response, long start) {
        if (response.getBody() != null && response.getBody().containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                String content = (String) message.get("content");
                if (content != null && !content.trim().isEmpty()) {
                    log.debug("LLM调用成功: 耗时={}ms, content长度={}", System.currentTimeMillis() - start, content.length());
                    return content;
                }
                // ⚠️ 禁止把思考过程(reasoning_content)当答案返回：
                // 思考内容是模型推理草稿，直接返回会造成内容泄漏（如每日简报夹带<think>推理过程）。
                // content 为空说明本次调用未产出有效答案，返回 null 由上层降级（规则模板/无思考重试）。
                Object reasoning = message.get("reasoning_content");
                int reasoningLen = reasoning != null ? String.valueOf(reasoning).length() : 0;
                log.warn("LLM content为空(reasoning_content长度={})，不返回思考内容，本次调用判定失败。耗时={}ms",
                    reasoningLen, System.currentTimeMillis() - start);
                return null;
            }
        }
        log.warn("LLM 返回格式异常: {}", response.getBody());
        return null;
    }

    @Override
    public boolean isAvailable() {
        return apiUrl != null && !apiUrl.isEmpty() && apiKey != null && !apiKey.isEmpty();
    }
}
