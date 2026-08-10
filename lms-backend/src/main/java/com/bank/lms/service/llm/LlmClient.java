package com.bank.lms.service.llm;

/**
 * LLM 客户端接口
 */
public interface LlmClient {

    /**
     * 发送对话请求
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return LLM 返回文本，不可用时返回 null
     */
    String chat(String systemPrompt, String userMessage);

    /**
     * 是否可用
     */
    boolean isAvailable();
}
