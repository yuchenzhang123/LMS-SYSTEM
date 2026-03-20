package com.bank.lms.repository;

import com.bank.lms.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知Repository
 */
@Repository
public interface NoticeRepository extends JpaRepository<Notice, String>, JpaSpecificationExecutor<Notice> {

    List<Notice> findByIsReadFalseOrderByCreatedAtDesc();

    List<Notice> findByIsReadTrueOrderByCreatedAtDesc();

    List<Notice> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Notice> findByLoanAccountOrderByCreatedAtDesc(String loanAccount);

    long countByIsReadFalse();

    @Modifying
    @Query("UPDATE Notice n SET n.isRead = true WHERE n.noticeId IN ?1")
    int markAsRead(List<String> noticeIds);
}
