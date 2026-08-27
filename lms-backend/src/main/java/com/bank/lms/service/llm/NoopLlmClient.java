package com.bank.lms.service.llm;

import lombok.extern.slf4j.Slf4j;

/**
 * 空 LLM 客户端（lms.llm.enabled=false 时使用）
 * 返回 null，触发规则模板降级
 */
@Slf4j
public class NoopLlmClient implements LlmClient {

    @Override
    public String chat(String systemPrompt, String userMessage) {
        log.debug("LLM未启用，返回空");
        return null;
    }

    @Override
    public String chat(String systemPrompt, String userMessage, boolean enableThinking) {
        log.debug("LLM未启用，返回空");
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
