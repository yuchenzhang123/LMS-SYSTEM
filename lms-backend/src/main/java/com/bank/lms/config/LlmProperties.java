package com.bank.lms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 配置属性（支持单 provider 与多 provider 主备）。
 *
 * providers[] 为空时回退到顶层单 provider 字段（向后兼容既有配置）。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "lms.llm")
public class LlmProperties {

    /** AI 大模型总开关，默认关闭 */
    private boolean enabled = false;

    /** 单 provider 顶层字段（providers 为空时使用，向后兼容） */
    private String apiUrl = "";
    private String apiKey = "";
    private String model = "gpt-4o-mini";
    private int maxTokens = 4096;
    private boolean enableThinking = true;
    /** JSON 输出模式：prompt（仅 prompt 约束，最保守）| response_format（附加 response_format 参数） */
    private String jsonMode = "prompt";

    /** 多 provider 列表（优先于单 provider 顶层字段，主失败自动切备） */
    private List<Provider> providers = new ArrayList<>();

    /** 单个 provider 配置 */
    @Data
    public static class Provider {
        private String apiUrl = "";
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private int maxTokens = 4096;
        private boolean enableThinking = true;
        private String jsonMode = "prompt";
    }
}
