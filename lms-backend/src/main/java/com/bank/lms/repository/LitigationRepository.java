package com.bank.lms.repository;

import com.bank.lms.entity.Litigation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 诉讼信息Repository
 */
@Repository
public interface LitigationRepository extends JpaRepository<Litigation, String>, JpaSpecificationExecutor<Litigation> {

    List<Litigation> findByLoanAccountOrderByUpdatedAtDesc(String loanAccount);

    List<Litigation> findByCustomerIdOrderByUpdatedAtDesc(String customerId);

    List<Litigation> findByStatusCode(String statusCode);

    List<Litigation> findByInLitigationTrue();

    @Query("SELECT l FROM Litigation l WHERE l.loanAccount = ?1 ORDER BY l.updatedAt DESC")
    List<Litigation> findLatestByLoanAccount(String loanAccount);
}
