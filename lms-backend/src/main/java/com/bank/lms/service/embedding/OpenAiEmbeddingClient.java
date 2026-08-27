package com.bank.lms.service.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 embedding 客户端（调用 /embeddings 接口）。
 * 通义千问、BGE、Qwen3-Embedding 等国内外模型 API 均兼容此格式。
 */
@Slf4j
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public OpenAiEmbeddingClient(RestTemplate restTemplate, String apiUrl, String apiKey, String model) {
        // 复用 LLM 客户端的超时策略：连接 5s / 读 60s，避免服务挂起阻塞业务线程
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(60000);
        restTemplate.setRequestFactory(factory);

        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public float[] embed(String text) {
        List<float[]> result = doEmbed(text, null);
        return result == null || result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return null;
        }
        return doEmbed(null, texts);
    }

    private List<float[]> doEmbed(String singleText, List<String> batchTexts) {
        if (!isAvailable()) {
            return null;
        }
        long start = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", singleText != null ? singleText : batchTexts);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            log.debug("embedding 调用成功: model={}, 耗时={}ms", model, System.currentTimeMillis() - start);
            return extractEmbeddings(response);
        } catch (Exception e) {
            log.error("embedding 调用失败: url={}, 耗时={}ms, 异常={}",
                apiUrl, System.currentTimeMillis() - start, e.toString());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<float[]> extractEmbeddings(ResponseEntity<Map> response) {
        if (response.getBody() == null || !response.getBody().containsKey("data")) {
            log.warn("embedding 返回格式异常: {}", response.getBody());
            return null;
        }
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        if (data == null || data.isEmpty()) {
            return null;
        }
        List<float[]> result = new ArrayList<>(data.size());
        for (Map<String, Object> item : data) {
            List<Double> embedding = (List<Double>) item.get("embedding");
            if (embedding == null) {
                continue;
            }
            float[] vec = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vec[i] = embedding.get(i).floatValue();
            }
            result.add(vec);
        }
        return result;
    }

    @Override
    public boolean isAvailable() {
        return apiUrl != null && !apiUrl.isEmpty() && apiKey != null && !apiKey.isEmpty();
    }
}
