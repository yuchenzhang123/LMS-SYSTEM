package com.bank.lms.repository;

import com.bank.lms.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 贷款账户Repository
 */
@Repository
public interface LoanAccountRepository extends JpaRepository<LoanAccount, String>, JpaSpecificationExecutor<LoanAccount> {

    List<LoanAccount> findByCustomerId(String customerId);

    List<LoanAccount> findByStatus(String status);

    List<LoanAccount> findByOverdueDaysGreaterThanEqual(Integer overdueDays);

    List<LoanAccount> findByProductCode(String productCode);
}
