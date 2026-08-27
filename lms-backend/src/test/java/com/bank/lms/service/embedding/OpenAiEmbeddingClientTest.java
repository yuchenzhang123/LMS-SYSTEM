package com.bank.lms.service.embedding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * OpenAiEmbeddingClient 响应解析单测 — mock RestTemplate，不发起真实网络调用。
 *
 * 验证 /embeddings 响应（data[].embedding 为 List<Double>）→ float[] 的转换，
 * 以及返回格式异常/网络异常时的 null 降级。
 */
@DisplayName("OpenAiEmbeddingClient 响应解析")
class OpenAiEmbeddingClientTest {

    private static final String URL = "http://x/v1/embeddings";
    private static final String KEY = "sk-test";

    private RestTemplate restTemplate;
    private OpenAiEmbeddingClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new OpenAiEmbeddingClient(restTemplate, URL, KEY, "text-embedding-3-small");
    }

    /** 构造 /embeddings 响应体（支持多条向量） */
    private Map<String, Object> responseBody(double[]... vecs) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (double[] vec : vecs) {
            Map<String, Object> item = new HashMap<>();
            List<Double> embedding = new ArrayList<>();
            for (double d : vec) {
                embedding.add(d);
            }
            item.put("embedding", embedding);
            data.add(item);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        return body;
    }

    @Test
    @DisplayName("embed：解析 data[0].embedding 为 float[]")
    void embedParses() {
        when(restTemplate.postForEntity(eq(URL), any(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(responseBody(new double[]{0.1, 0.2, 0.3}), HttpStatus.OK));

        float[] vec = client.embed("逾期政策");
        assertNotNull(vec);
        assertEquals(3, vec.length);
        assertArrayEquals(new float[]{0.1f, 0.2f, 0.3f}, vec, 0.0001f);
    }

    @Test
    @DisplayName("embedBatch：解析多条向量")
    void embedBatchParses() {
        when(restTemplate.postForEntity(eq(URL), any(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(
                responseBody(new double[]{0.1, 0.2}, new double[]{0.3, 0.4}), HttpStatus.OK));

        List<float[]> vecs = client.embedBatch(Arrays.asList("a", "b"));
        assertNotNull(vecs);
        assertEquals(2, vecs.size());
        assertArrayEquals(new float[]{0.1f, 0.2f}, vecs.get(0), 0.0001f);
        assertArrayEquals(new float[]{0.3f, 0.4f}, vecs.get(1), 0.0001f);
    }

    @Test
    @DisplayName("响应缺 data 字段 → null 降级")
    void embedMalformedResponseReturnsNull() {
        when(restTemplate.postForEntity(eq(URL), any(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(new HashMap<String, Object>(), HttpStatus.OK));

        assertNull(client.embed("逾期政策"));
    }

    @Test
    @DisplayName("网络异常 → null 降级")
    void embedOnExceptionReturnsNull() {
        when(restTemplate.postForEntity(eq(URL), any(), eq(Map.class)))
            .thenThrow(new RuntimeException("连接失败"));

        assertNull(client.embed("逾期政策"));
    }

    @Test
    @DisplayName("embedBatch 空列表 → null")
    void embedBatchEmptyReturnsNull() {
        assertNull(client.embedBatch(new ArrayList<String>()));
    }

    @Test
    @DisplayName("isAvailable：需同时具备 api-url 与 api-key")
    void isAvailableChecksUrlAndKey() {
        assertTrue(client.isAvailable());
        assertFalse(new OpenAiEmbeddingClient(restTemplate, "", KEY, "m").isAvailable());
        assertFalse(new OpenAiEmbeddingClient(restTemplate, URL, "", "m").isAvailable());
    }
}
