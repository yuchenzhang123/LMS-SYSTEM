package com.bank.lms.service.llm;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Function;

/**
 * 多 provider 路由客户端：按序尝试各 provider，主失败自动切备（含超时/异常兜底）。
 *
 * 每个 provider 不可用（isAvailable=false）或返回空时跳过，直到某个 provider 返回非空结果；
 * 全部失败返回 null。用于 LLM 故障/超时时保证可用性。
 */
@Slf4j
public class RoutingLlmClient implements LlmClient {

    private final List<LlmClient> providers;

    public RoutingLlmClient(List<LlmClient> providers) {
        this.providers = providers;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        return route(p -> p.chat(systemPrompt, userMessage));
    }

    @Override
    public String chat(String systemPrompt, String userMessage, boolean enableThinking) {
        return route(p -> p.chat(systemPrompt, userMessage, enableThinking));
    }

    @Override
    public String chatJson(String systemPrompt, String userMessage) {
        return route(p -> p.chatJson(systemPrompt, userMessage));
    }

    @Override
    public boolean isAvailable() {
        return providers != null && providers.stream().anyMatch(LlmClient::isAvailable);
    }

    /** 按序尝试各 provider，返回首个非空结果；全部失败返回 null */
    private String route(Function<LlmClient, String> call) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        for (int i = 0; i < providers.size(); i++) {
            LlmClient p = providers.get(i);
            if (!p.isAvailable()) {
                continue;
            }
            String result = call.apply(p);
            if (result != null && !result.trim().isEmpty()) {
                if (i > 0) {
                    log.info("LLM 主 provider 失败，已切换到备 provider[{}]", i);
                }
                return result;
            }
        }
        return null;
    }
}
