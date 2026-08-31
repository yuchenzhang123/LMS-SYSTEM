package com.bank.lms.unit;

import com.bank.lms.service.llm.LlmClient;
import com.bank.lms.service.llm.OpenAiLlmClient;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * LLM 连通性测试 — 纯 JUnit，不依赖 Spring 上下文（无 @Autowired，无 bean 注入问题）
 *
 * 配置来源（优先级从高到低）：
 *   1. 命令行参数: ./step4-llm.sh <url> <key> [model]
 *   2. 外置配置:   config/application-test.yml（测试机 scripts/config/ 下，覆盖内置）
 *   3. 内置配置:   application-test.yml（主JAR内，取 lms.llm.* 段；占位符视为未配置则跳过）
 *
 * 思考开关按功能独立控制（默认与 application.yml 的 lms.llm.thinking.* 对齐）：
 *   -Dlms.llm.thinking.ask=true      -Dlms.llm.thinking.briefing=false
 *   -Dlms.llm.thinking.summary=false
 *
 * 全部未配置时测试跳过并提示用法。
 */
@DisplayName("LLM 连通性测试")
class LlmConnectivityTest {

    private static LlmClient llmClient;
    private static String configuredUrl;
    private static String configuredModel;
    /** 按功能独立思考开关（默认对齐 CopilotService：ask 开，其余关） */
    private static boolean thinkingAsk;
    private static boolean thinkingBriefing;
    private static boolean thinkingSummary;

    @BeforeAll
    static void setUp() {
        Map<String, String> cfg = loadConfig();
        if (cfg == null) {
            llmClient = null;
            return;
        }
        configuredUrl = cfg.get("api-url");
        configuredModel = cfg.get("model");
        // 推理模型推理过程耗 token，max_tokens 给大点
        int maxTokens = Integer.parseInt(System.getProperty("lms.llm.max-tokens", "4096"));
        boolean defaultEnableThinking = Boolean.parseBoolean(System.getProperty("lms.llm.enable-thinking", "true"));
        // 各功能思考开关，默认与 application.yml 的 lms.llm.thinking.* 保持一致
        thinkingAsk = Boolean.parseBoolean(System.getProperty("lms.llm.thinking.ask", "true"));
        thinkingBriefing = Boolean.parseBoolean(System.getProperty("lms.llm.thinking.briefing", "false"));
        thinkingSummary = Boolean.parseBoolean(System.getProperty("lms.llm.thinking.summary", "false"));
        llmClient = new OpenAiLlmClient(
            new RestTemplate(),
            configuredUrl,
            cfg.get("api-key"),
            configuredModel,
            maxTokens,
            defaultEnableThinking,
            System.getProperty("lms.llm.json-mode", "prompt")
        );
        System.out.println("=== LLM 连通性测试 ===");
        System.out.println("URL:   " + configuredUrl);
        System.out.println("Model: " + configuredModel);
        System.out.println("MaxTokens: " + maxTokens);
        System.out.println("Thinking: ask=" + thinkingAsk + ", briefing=" + thinkingBriefing
            + ", summary=" + thinkingSummary);
    }

    /** 三级配置加载 */
    private static Map<String, String> loadConfig() {
        // 1. 命令行 -D 参数（step4 脚本传入）
        String url = System.getProperty("lms.llm.api-url");
        String key = System.getProperty("lms.llm.api-key");
        String model = System.getProperty("lms.llm.model", "");
        if (notEmpty(url) && notEmpty(key)) {
            Map<String, String> m = new HashMap<>();
            m.put("api-url", url.trim());
            m.put("api-key", key.trim());
            m.put("model", notEmpty(model) ? model.trim() : "qwen-plus");
            return m;
        }
        // 2. 外置配置 config/application-test.yml（测试机 scripts/config/ 下，覆盖内置）
        Map<String, String> ext = parseYamlFile("config/application-test.yml");
        if (ext != null) return ext;
        // 3. 内置配置 application-test.yml（主JAR内，lms.llm.* 段）
        return parseYamlClasspath("application-test.yml");
    }

    private static Map<String, String> parseYamlFile(String path) {
        try (InputStream in = new FileInputStream(path)) {
            return extractLlmConfig(in);
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, String> parseYamlClasspath(String path) {
        try (InputStream in = LlmConnectivityTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) return null;
            return extractLlmConfig(in);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> extractLlmConfig(InputStream in) {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(in);
        Map<String, Object> lms = (Map<String, Object>) root.get("lms");
        Map<String, Object> llm = (Map<String, Object>) lms.get("llm");
        Map<String, String> result = new HashMap<>();
        result.put("api-url", String.valueOf(llm.get("api-url")));
        result.put("api-key", String.valueOf(llm.get("api-key")));
        result.put("model", llm.get("model") != null ? String.valueOf(llm.get("model")) : "qwen-plus");
        String keyVal = result.get("api-key");
        // 占位符/模板默认值视为未配置（含 <内网IP> 尖括号占位符）
        if (!notEmpty(result.get("api-url")) || !notEmpty(keyVal)
                || result.get("api-url").contains("<") || result.get("api-url").contains(">")
                || keyVal.contains("<") || keyVal.contains(">")
                || keyVal.contains("${") || "sk-your-key-here".equals(keyVal)) {
            return null;
        }
        return result;
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** 未配置时跳过并给出用法提示 */
    private void assumeConfigured() {
        Assumptions.assumeTrue(llmClient != null,
            "\n未配置LLM参数。用法之一：\n" +
            "  ./step4-llm.sh http://千问地址/v1/chat/completions sk-xxx qwen-model\n" +
            "或在 scripts/config/application-test.yml 配置 lms.llm 段（参考主JAR内置 application-test.yml）");
    }

    private static final String NULL_MSG =
        "\n⚠️ LLM 调用返回空。可能原因：\n" +
        "  1. api-url 配错或不可达（先用 curl 直接测接口）\n" +
        "  2. api-key 无效或过期\n" +
        "  3. 网络策略未放行\n" +
        "  4. 模型名不存在\n" +
        "  5. 推理模型 max_tokens 不足（content为空，已尝试回退reasoning_content）\n" +
        "  详情看上方 'LLM 调用失败' / 'LLM 返回格式异常' 日志";

    @Test @DisplayName("1. 客户端可用")
    void clientAvailable() {
        assumeConfigured();
        assertThat(llmClient.isAvailable()).isTrue();
        System.out.println("✅ LLM 客户端可用");
    }

    @Test @DisplayName("2. 简单对话")
    void basicChat() {
        assumeConfigured();
        String response = llmClient.chat(
            "你是一个测试助手。",
            "请回复：连通性测试通过",
            thinkingAsk
        );
        System.out.println("响应: " + response);

        assertThat(response).as(NULL_MSG).isNotNull().isNotEmpty();
        assertThat(response).containsIgnoringCase("通过");
        System.out.println("✅ 简单对话正常");
    }

    @Test @DisplayName("3. 催收简报生成")
    void briefingGeneration() {
        assumeConfigured();
        String response = llmClient.chat(
            "你是银行贷后催收管理系统的数据分析助手。请用中文简洁回答。",
            "根据以下数据生成一段80字以内的每日简报：\n" +
            "全行逾期账户1,284笔，较昨日+12笔。逾期总余额3.2亿。催收完成率72%。\n" +
            "海秀支行逾期率连续3天上升(+8%)，龙华支行催收完成率创新高(85%)。",
            thinkingBriefing
        );
        System.out.println("简报: " + response);

        assertThat(response).as(NULL_MSG).isNotNull().isNotEmpty();
        System.out.println("✅ 简报生成正常");
    }

    @Test @DisplayName("4. 催收摘要生成")
    void collectionSummary() {
        assumeConfigured();
        String response = llmClient.chat(
            "你是银行贷后催收管理系统的数据分析助手。请用中文简洁回答。",
            "以下是账户的催收记录，请用60字以内总结催收历程：\n" +
            "- 2026-07-15 (电话): 客户称下周发工资再还\n" +
            "- 2026-07-28 (短信): 已发送还款提醒\n" +
            "- 2026-08-05 (上门): 客户承诺本月内还清\n" +
            "- 2026-08-10 (电话): 无人接听",
            thinkingSummary
        );
        System.out.println("摘要: " + response);

        assertThat(response).as(NULL_MSG).isNotNull().isNotEmpty();
        System.out.println("✅ 摘要生成正常");
    }

    @Test @DisplayName("5. 超长消息不崩溃")
    void longMessage() {
        assumeConfigured();
        StringBuilder sb = new StringBuilder(); for (int i = 0; i < 200; i++) sb.append("账户逾期信息 "); String longText = "测试数据: " + sb.toString();
        String response = llmClient.chat("请简洁回答", longText, thinkingAsk);
        int len = response != null ? response.length() : 0;
        System.out.println("超长消息响应长度: " + len);
        // 不崩溃就算通过
        System.out.println("✅ 超长消息处理正常");
    }
}
