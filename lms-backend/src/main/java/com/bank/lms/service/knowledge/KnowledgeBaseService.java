package com.bank.lms.service.knowledge;

import com.bank.lms.config.EmbeddingProperties;
import com.bank.lms.entity.KnowledgeBase;
import com.bank.lms.repository.KnowledgeBaseRepository;
import com.bank.lms.service.embedding.EmbeddingClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库服务：CRUD 编排（切块 → 向量化 → 落库 → 刷新内存索引）+ 向量召回。
 *
 * 「知识条目」以 title 为业务标识，物理上对应 1..N 行（N 个切块）。
 * 写操作完成后调用 {@link KnowledgeVectorStore#refresh()} 重建内存索引。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository repository;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingProperties properties;
    private final KnowledgeVectorStore vectorStore;
    private final ObjectMapper objectMapper;

    // ==================== 查询 ====================

    /** 按标题聚合的条目列表（不含向量大字段） */
    public List<Map<String, Object>> list() {
        List<Object[]> rows = repository.listGrouped();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", row[0]);
            item.put("category", row[1]);
            item.put("updatedAt", row[2]);
            item.put("chunkCount", row[3]);
            result.add(item);
        }
        return result;
    }

    /** 向量召回（委托内存索引） */
    public List<KnowledgeVectorStore.SearchHit> search(String query) {
        return vectorStore.search(query);
    }

    /** 查询某标题下的原文（合并所有切块按序号拼接，供编辑回显） */
    public Map<String, Object> getByTitle(String title) {
        List<KnowledgeBase> chunks = repository.findByTitle(title);
        chunks.sort((a, b) -> Integer.compare(
            a.getChunkIndex() != null ? a.getChunkIndex() : 0,
            b.getChunkIndex() != null ? b.getChunkIndex() : 0));
        StringBuilder content = new StringBuilder();
        String category = null;
        for (KnowledgeBase chunk : chunks) {
            if (chunk.getContent() != null) {
                content.append(chunk.getContent());
            }
            if (category == null && chunk.getCategory() != null) {
                category = chunk.getCategory();
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", title);
        result.put("category", category);
        result.put("content", content.toString());
        return result;
    }

    // ==================== 写 ====================

    /** 新增文本条目：切块 + 向量化 + 落库 */
    @Transactional
    public void addText(String title, String category, String content) {
        requireTitleAndContent(title, content);
        List<String> chunks = TextChunker.chunk(content.trim(), properties.getChunkSize(), properties.getChunkOverlap());
        saveChunks(title.trim(), category, chunks);
        vectorStore.refresh();
    }

    /** 上传文件导入：解析 + 切块 + 向量化 + 落库 */
    @Transactional
    public void importFile(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String filename = file.getOriginalFilename();
        String title = stripExtension(filename);
        String text;
        try {
            text = DocumentParser.extract(file.getBytes(), filename);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("文件读取失败: {}", e.toString());
            throw new IllegalArgumentException("文件读取失败");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("文件中未解析出文本内容");
        }
        List<String> chunks = TextChunker.chunk(text.trim(), properties.getChunkSize(), properties.getChunkOverlap());
        saveChunks(title, category, chunks);
        vectorStore.refresh();
    }

    /** 编辑：软删旧切块后重新切块 + 向量化 */
    @Transactional
    public void update(String title, String category, String content) {
        requireTitleAndContent(title, content);
        repository.softDeleteByTitle(title.trim());
        List<String> chunks = TextChunker.chunk(content.trim(), properties.getChunkSize(), properties.getChunkOverlap());
        saveChunks(title.trim(), category, chunks);
        vectorStore.refresh();
    }

    /** 删除：软删该标题下所有切块 */
    @Transactional
    public void delete(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        int n = repository.softDeleteByTitle(title.trim());
        log.info("删除知识条目: title={}, 影响行数={}", title.trim(), n);
        vectorStore.refresh();
    }

    // ==================== 私有 ====================

    private void requireTitleAndContent(String title, String content) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("内容不能为空");
        }
    }

    /** 向量化 chunks 并落库（embedding 不可用时仅落库文本，向量为空） */
    private void saveChunks(String title, String category, List<String> chunks) {
        List<float[]> vectors = embeddingClient.isAvailable() ? embeddingClient.embedBatch(chunks) : null;
        int total = chunks.size();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeBase kb = new KnowledgeBase();
            kb.setTitle(title);
            kb.setCategory(category);
            kb.setContent(chunks.get(i));
            kb.setChunkIndex(i);
            kb.setChunkTotal(total);
            if (vectors != null && i < vectors.size() && vectors.get(i) != null) {
                try {
                    kb.setEmbedding(objectMapper.writeValueAsString(vectors.get(i)));
                } catch (Exception e) {
                    log.warn("向量序列化失败: title={}, chunkIndex={}", title, i);
                }
            }
            repository.save(kb);
        }
    }

    private String stripExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "未命名";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
