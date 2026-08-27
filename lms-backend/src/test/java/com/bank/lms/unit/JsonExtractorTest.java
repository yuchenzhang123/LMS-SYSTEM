package com.bank.lms.unit;

import com.bank.lms.service.nl2sql.JsonExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 单元测试 — JSON 提取器（LLM 文本 → 干净 JSON）。
 */
@DisplayName("JSON 提取器 (单元测试)")
class JsonExtractorTest {

    @Test @DisplayName("提取纯净 JSON 原样返回")
    void extractsPlainJson() {
        String raw = "{\"intent\":\"nl2sql\",\"sql\":\"SELECT 1\"}";
        assertThat(JsonExtractor.extractJson(raw)).isEqualTo(raw);
    }

    @Test @DisplayName("剥离 markdown 围栏")
    void stripsMarkdownFence() {
        String raw = "```json\n{\"intent\":\"metric\",\"metricName\":\"ORG_RANKING\"}\n```";
        assertThat(JsonExtractor.extractJson(raw))
            .contains("\"intent\":\"metric\"")
            .doesNotContain("```");
    }

    @Test @DisplayName("剥离前后噪声文字")
    void stripsSurroundingNoise() {
        String raw = "好的，结果如下：{\"intent\":\"chat\"} 希望对你有帮助";
        assertThat(JsonExtractor.extractJson(raw)).isEqualTo("{\"intent\":\"chat\"}");
    }

    @Test @DisplayName("嵌套 JSON 正确截取到最后一个右花括号")
    void handlesNestedJson() {
        String raw = "{\"intent\":\"nl2sql\",\"params\":{\"a\":1,\"b\":{\"c\":2}}}";
        assertThat(JsonExtractor.extractJson(raw)).contains("\"c\":2");
    }

    @Test @DisplayName("空输入抛异常")
    void emptyThrows() {
        assertThatThrownBy(() -> JsonExtractor.extractJson(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JsonExtractor.extractJson("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test @DisplayName("无花括号抛异常")
    void noBracesThrows() {
        assertThatThrownBy(() -> JsonExtractor.extractJson("你好，今天天气不错"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test @DisplayName("非法 JSON（尾逗号）抛异常")
    void invalidJsonThrows() {
        assertThatThrownBy(() -> JsonExtractor.extractJson("{\"intent\": \"chat\",}"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
