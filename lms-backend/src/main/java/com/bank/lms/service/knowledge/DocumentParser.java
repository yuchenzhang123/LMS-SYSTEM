package com.bank.lms.service.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * 文档文本解析：按扩展名分发到成熟解析库。
 *   pdf  → Apache PDFBox 2.0.x（Java 8 兼容）
 *   docx → Apache POI XWPF
 *   doc  → Apache POI HWPF（poi-scratchpad）
 *   txt/md → 直接按 UTF-8 读取
 *
 * 未用 Apache Tika 统一解析：Tika 3.x 要求 Java 11（与本项目 1.8 冲突），2.x 依赖过重，
 * 直接引入 PDFBox + POI 两个轻量库更可控。
 */
@Slf4j
public final class DocumentParser {

    private DocumentParser() {
    }

    /**
     * 解析文档为纯文本。
     *
     * @param bytes    文件字节
     * @param filename 原始文件名（用于判断扩展名）
     * @return 解析出的纯文本；空文本返回空字符串
     * @throws IllegalArgumentException 不支持的文件格式或解析失败
     */
    public static String extract(byte[] bytes, String filename) {
        String ext = extension(filename);
        try {
            if ("pdf".equals(ext)) {
                return extractPdf(bytes);
            } else if ("docx".equals(ext)) {
                return extractDocx(bytes);
            } else if ("doc".equals(ext)) {
                return extractDoc(bytes);
            } else if ("txt".equals(ext) || "md".equals(ext)) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            throw new IllegalArgumentException("不支持的文件格式：" + ext);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("文档解析失败: filename={}, 异常={}", filename, e.toString());
            throw new IllegalArgumentException("文件解析失败，请确认文件未损坏且格式正确");
        }
    }

    private static String extractPdf(byte[] bytes) throws Exception {
        try (PDDocument document = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private static String extractDocx(byte[] bytes) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String extractDoc(byte[] bytes) throws Exception {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String extension(String filename) {
        if (filename == null || filename.lastIndexOf('.') < 0) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
