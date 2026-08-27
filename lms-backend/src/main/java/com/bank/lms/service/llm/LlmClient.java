package com.bank.lms.service.llm;

/**
 * LLM 客户端接口
 */
public interface LlmClient {

    /**
     * 发送对话请求（使用默认推理设置）
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return LLM 返回文本，不可用时返回 null
     */
    String chat(String systemPrompt, String userMessage);

    /**
     * 发送对话请求，按调用场景指定是否开启思考（推理）模式
     *
     * 适用建议：
     *   enableThinking=true  — 需要多步推理的场景（如业务归因分析问答）
     *   enableThinking=false — 简单直出的场景（意图识别/简报/摘要，省token提速）
     *
     * @return LLM 返回文本，不可用时返回 null
     */
    String chat(String systemPrompt, String userMessage, boolean enableThinking);

    /**
     * 是否可用
     */
    boolean isAvailable();

    /**
     * 请求 JSON 结构化输出（关闭思考模式）。
     * 返回原始文本（可能含 markdown 围栏/噪声），由调用方用 JsonExtractor 提取。
     * 默认实现退化为关闭思考的 chat；OpenAiLlmClient 可覆盖以附加 response_format。
     */
    default String chatJson(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, false);
    }
}
