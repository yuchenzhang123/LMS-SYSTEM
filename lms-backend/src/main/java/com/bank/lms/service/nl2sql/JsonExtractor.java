package com.bank.lms.service.nl2sql;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 从 LLM 文本输出中提取 JSON 对象。
 * 不假定内网 32B 支持 response_format，容错处理 markdown 围栏与前后噪声。
 */
public final class JsonExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonExtractor() {
    }

    /**
     * 提取并校验 JSON 对象字符串。
     * @return 干净的 JSON 字符串（可被 ObjectMapper.readValue 解析）
     * @throws IllegalArgumentException 提取或解析失败（message 可回喂 LLM 修正）
     */
    public static String extractJson(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("LLM 返回为空");
        }
        String s = raw.trim();

        // 剥离 markdown 围栏 ```json ... ```
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline >= 0) {
                s = s.substring(firstNewline + 1);
            }
            int lastFence = s.lastIndexOf("```");
            if (lastFence >= 0) {
                s = s.substring(0, lastFence);
            }
            s = s.trim();
        }

        // 找第一个 { 与最后一个 }，截取 JSON 对象
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalArgumentException("未找到 JSON 对象");
        }
        String json = s.substring(start, end + 1);

        // Jackson 校验合法性
        try {
            MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 解析失败: " + e.getMessage());
        }
        return json;
    }
}
