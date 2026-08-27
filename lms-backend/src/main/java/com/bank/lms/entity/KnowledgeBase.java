package com.bank.lms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * 知识库实体（RAG 向量召回）。
 *
 * 一条「知识条目」物理上对应 1..N 行（N 个切块 chunk），每行存一个切片的正文及其向量。
 * embedding 用 Jackson 把 float[] 序列化为 JSON 字符串存 TEXT 列（MySQL/GaussDB 双库通用，
 * 避免 JSON 类型的 CAST 差异），召回时反序列化回 float[] 在内存计算余弦相似度。
 */
@Data
@Entity
@Table(name = "knowledge_base")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeBase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 知识标题（文本条目的标题，或上传文件名） */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    /** 分类（可选） */
    @Column(name = "category", length = 50)
    private String category;

    /** 片段正文（切块后的文本，整条不分块时即全文） */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** 切块序号（整条不分块时 = 0） */
    @Column(name = "chunk_index")
    private Integer chunkIndex = 0;

    /** 该条目的总块数 */
    @Column(name = "chunk_total")
    private Integer chunkTotal = 1;

    /** 向量序列化 JSON 数组（float[]），如 "[0.12,-0.03,...]" */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;
}
