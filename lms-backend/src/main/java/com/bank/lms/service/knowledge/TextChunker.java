package com.bank.lms.service.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本切块工具（手写递归字符切块，RAG 用）。
 *
 * 为什么手写而非引入现成切块库：
 *   LangChain4j（Java 端 RecursiveCharacterTextSplitter 的实现）要求 JDK 17+，与本项目 Java 1.8 冲突；
 *   且切块是简单算法，按「句末标点优先、超长再硬切、相邻块 overlap」实现即可，无需引入整框架。
 *
 * 策略（中文 RAG 最佳实践）：先按句末标点（。！？；\n）切句，贪心合并句子到接近 chunkSize，
 * 单句超长时按 chunkSize 硬切，相邻块之间保留 overlap 字符维持上下文连贯。
 */
public final class TextChunker {

    /** 句子边界：匹配一个或多个非标点字符 + 可选句末标点 */
    private static final Pattern SENTENCE = Pattern.compile("[^。！？；\\n]+[。！？；\\n]?");

    private TextChunker() {
    }

    /**
     * 把长文本切成若干块。
     *
     * @param text        原文
     * @param chunkSize   块大小上限（字符数）
     * @param chunkOverlap 相邻块重叠字符数（<=0 表示不重叠）
     * @return 切分后的块列表；空文本返回空列表
     */
    public static List<String> chunk(String text, int chunkSize, int chunkOverlap) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String trimmed = text.trim();
        if (chunkSize <= 0) {
            return Collections.singletonList(trimmed);
        }
        if (trimmed.length() <= chunkSize) {
            return Collections.singletonList(trimmed);
        }

        // 1. 按句末标点切句
        List<String> sentences = splitSentences(trimmed);

        // 2. 贪心合并句子成块
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (sentence.length() > chunkSize) {
                // 单句超长：先落盘当前块，再硬切超长句
                if (current.length() > 0) {
                    chunks.add(current.toString());
                    current = new StringBuilder();
                }
                chunks.addAll(hardSplit(sentence, chunkSize));
                continue;
            }
            if (current.length() + sentence.length() > chunkSize) {
                chunks.add(current.toString());
                current = new StringBuilder(sentence);
            } else {
                current.append(sentence);
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }

        // 3. 应用 overlap（前一块末尾 overlap 字符拼到后一块开头）
        return applyOverlap(chunks, chunkOverlap);
    }

    private static List<String> splitSentences(String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = SENTENCE.matcher(text);
        while (matcher.find()) {
            String sentence = matcher.group();
            if (!sentence.trim().isEmpty()) {
                result.add(sentence);
            }
        }
        if (result.isEmpty()) {
            result.add(text);
        }
        return result;
    }

    /** 超长句按 chunkSize 硬切（无语义边界可用时的兜底） */
    private static List<String> hardSplit(String text, int chunkSize) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            result.add(text.substring(start, end));
            start = end;
        }
        return result;
    }

    private static List<String> applyOverlap(List<String> chunks, int overlap) {
        if (chunks.size() <= 1 || overlap <= 0) {
            return chunks;
        }
        List<String> result = new ArrayList<>();
        result.add(chunks.get(0));
        for (int i = 1; i < chunks.size(); i++) {
            String prev = chunks.get(i - 1);
            String cur = chunks.get(i);
            int take = Math.min(overlap, prev.length());
            result.add(prev.substring(prev.length() - take) + cur);
        }
        return result;
    }
}
