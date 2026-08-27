package com.bank.lms.controller;

import com.bank.lms.common.Result;
import com.bank.lms.service.knowledge.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库控制器（RAG 向量召回）。
 * 写操作（新增/编辑/删除/导入）前端按 admin/manager 角色控制入口。
 */
@Slf4j
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 知识条目列表（按标题聚合，不含向量大字段）
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.success(knowledgeBaseService.list());
    }

    /**
     * 查询某标题原文（编辑回显）
     */
    @GetMapping("/{title}")
    public Result<Map<String, Object>> getByTitle(@PathVariable String title) {
        return Result.success(knowledgeBaseService.getByTitle(title));
    }

    /**
     * 新增文本知识条目
     */
    @PostMapping
    public Result<String> add(@RequestBody Map<String, String> body) {
        try {
            knowledgeBaseService.addText(body.get("title"), body.get("category"), body.get("content"));
            return Result.success("新增知识成功");
        } catch (IllegalArgumentException e) {
            log.warn("新增知识失败: {}", e.getMessage());
            return Result.error("400", e.getMessage());
        }
    }

    /**
     * 上传文件导入（pdf/doc/docx/txt/md）
     */
    @PostMapping("/import")
    public Result<String> importFile(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "category", required = false) String category) {
        try {
            knowledgeBaseService.importFile(file, category);
            return Result.success("导入知识成功");
        } catch (IllegalArgumentException e) {
            log.warn("导入知识失败: {}", e.getMessage());
            return Result.error("400", e.getMessage());
        }
    }

    /**
     * 编辑知识条目（软删旧切块后重新切块向量化）
     */
    @PutMapping("/{title}")
    public Result<String> update(@PathVariable String title, @RequestBody Map<String, String> body) {
        try {
            knowledgeBaseService.update(title, body.get("category"), body.get("content"));
            return Result.success("更新知识成功");
        } catch (IllegalArgumentException e) {
            log.warn("更新知识失败: {}", e.getMessage());
            return Result.error("400", e.getMessage());
        }
    }

    /**
     * 删除知识条目（软删该标题下所有切块）
     */
    @DeleteMapping("/{title}")
    public Result<String> delete(@PathVariable String title) {
        try {
            knowledgeBaseService.delete(title);
            return Result.success("删除知识成功");
        } catch (IllegalArgumentException e) {
            log.warn("删除知识失败: {}", e.getMessage());
            return Result.error("400", e.getMessage());
        }
    }
}
