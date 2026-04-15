package com.bank.lms.repository;

import com.bank.lms.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    // gracePeriod=0 表示已还款（不再逾期），collecting/uncollected 均需转 completed，用于兜底检查
    @Query("SELECT a FROM LoanAccount a WHERE a.status IN ('collecting', 'uncollected') AND a.isDeleted = 0 AND (a.extraData LIKE '%\"gracePeriod\":0%' OR a.extraData LIKE '%\"gracePeriod\": 0%')")
    List<LoanAccount> findActiveWithGracePeriodZero();

    @Modifying
    @Query("UPDATE LoanAccount a SET a.status = :newStatus, a.statusUpdateTime = :now WHERE a.loanAccount IN :ids AND a.isDeleted = 0")
    int bulkUpdateStatusByIds(@Param("ids") List<String> ids,
                              @Param("newStatus") String newStatus,
                              @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(a), SUM(a.loanBalance) FROM LoanAccount a WHERE a.status IN ('uncollected', 'collecting') AND a.isDeleted = 0")
    List<Object[]> statsActiveAll();

    @Query("SELECT COUNT(a), SUM(a.loanBalance) FROM LoanAccount a WHERE a.status IN ('uncollected', 'collecting') AND a.isDeleted = 0 AND a.branchCode = :branchCode")
    List<Object[]> statsActiveByBranchCode(@Param("branchCode") String branchCode);

    @Query("SELECT COUNT(a), SUM(a.loanBalance) FROM LoanAccount a WHERE a.status IN ('uncollected', 'collecting') AND a.isDeleted = 0 AND a.branchCode IN :branchCodes")
    List<Object[]> statsActiveByBranchCodes(@Param("branchCodes") List<String> branchCodes);
}
