package com.bank.lms.repository;

import com.bank.lms.entity.AiQueryAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiQueryAuditLogRepository extends JpaRepository<AiQueryAuditLog, Long> {
}
