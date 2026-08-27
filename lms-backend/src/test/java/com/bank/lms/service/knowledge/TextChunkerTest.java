package com.bank.lms.service.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    @Test
    void 空文本返回空列表() {
        assertTrue(TextChunker.chunk("", 100, 10).isEmpty());
        assertTrue(TextChunker.chunk(null, 100, 10).isEmpty());
    }

    @Test
    void 短文本不分块() {
        List<String> chunks = TextChunker.chunk("这是一段很短的文本。", 100, 10);
        assertEquals(1, chunks.size());
        assertEquals("这是一段很短的文本。", chunks.get(0));
    }

    @Test
    void 长文本切成多块且每块不超上限() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("第").append(i).append("句话，用于测试切块逻辑。");
        }
        List<String> chunks = TextChunker.chunk(sb.toString(), 50, 0);
        assertTrue(chunks.size() > 1, "长文本应切成多块");
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 50, "每块长度不应超过 chunkSize=" + chunk.length());
        }
    }

    @Test
    void 相邻块有重叠() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            sb.append("第").append(i).append("句内容比较长用于制造多块切分。");
        }
        int overlap = 8;
        List<String> chunks = TextChunker.chunk(sb.toString(), 40, overlap);
        assertTrue(chunks.size() > 1);
        for (int i = 1; i < chunks.size(); i++) {
            String prev = chunks.get(i - 1);
            String cur = chunks.get(i);
            int take = Math.min(overlap, prev.length());
            assertTrue(cur.startsWith(prev.substring(prev.length() - take)),
                "后一块应以「前一块末尾 overlap 字符」开头");
        }
    }
}
