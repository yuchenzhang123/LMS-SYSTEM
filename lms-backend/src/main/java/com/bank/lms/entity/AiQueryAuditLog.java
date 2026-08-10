package com.bank.lms.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * AI 查询审计日志
 */
@Data
@Entity
@Table(name = "ai_query_audit_log")
public class AiQueryAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ehr_no", length = 50, nullable = false)
    private String ehrNo;

    @Column(name = "org_code", length = 20, nullable = false)
    private String orgCode;

    @Column(name = "question", columnDefinition = "TEXT")
    private String question;

    @Column(name = "capability", length = 50)
    private String capability;

    @Column(name = "params", columnDefinition = "JSON")
    private String params;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
