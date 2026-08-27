package com.bank.lms.service.knowledge;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentParserTest {

    @Test
    void txt按UTF8解析() {
        String text = DocumentParser.extract("这是一段文本内容。".getBytes(StandardCharsets.UTF_8), "test.txt");
        assertEquals("这是一段文本内容。", text);
    }

    @Test
    void md按UTF8解析() {
        String text = DocumentParser.extract("# 标题\n正文内容".getBytes(StandardCharsets.UTF_8), "readme.md");
        assertEquals("# 标题\n正文内容", text);
    }

    @Test
    void 不支持格式抛异常() {
        assertThrows(IllegalArgumentException.class,
            () -> DocumentParser.extract(new byte[]{1, 2, 3}, "test.xyz"));
    }

    @Test
    void 无扩展名抛异常() {
        assertThrows(IllegalArgumentException.class,
            () -> DocumentParser.extract(new byte[]{1, 2, 3}, "test"));
    }
}
