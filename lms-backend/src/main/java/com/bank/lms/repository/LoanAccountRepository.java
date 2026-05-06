package com.bank.lms.repository;

import com.bank.lms.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 贷款账户Repository
 */
@Repository
public interface LoanAccountRepository extends JpaRepository<LoanAccount, String>, JpaSpecificationExecutor<LoanAccount> {

    @Query("SELECT COUNT(a), SUM(a.loanBalance) FROM LoanAccount a WHERE a.status IN ('uncollected', 'collecting') AND a.isDeleted = 0")
    List<Object[]> statsActiveAll();

    @Query("SELECT COUNT(a), SUM(a.loanBalance) FROM LoanAccount a WHERE a.status IN ('uncollected', 'collecting') AND a.isDeleted = 0 AND a.branchCode = :branchCode")
    List<Object[]> statsActiveByBranchCode(@Param("branchCode") String branchCode);

    @Query("SELECT COUNT(a), SUM(a.loanBalance) FROM LoanAccount a WHERE a.status IN ('uncollected', 'collecting') AND a.isDeleted = 0 AND a.branchCode IN :branchCodes")
    List<Object[]> statsActiveByBranchCodes(@Param("branchCodes") List<String> branchCodes);

    /**
     * 按账户号批量查询（用于同步时批量比对，替代逐条 findById）
     */
    @Query("SELECT a FROM LoanAccount a WHERE a.loanAccount IN :ids")
    List<LoanAccount> findAllByLoanAccountIn(@Param("ids") Collection<String> ids);
}
