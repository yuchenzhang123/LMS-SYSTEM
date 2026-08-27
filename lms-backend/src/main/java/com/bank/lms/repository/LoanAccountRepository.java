package com.bank.lms.repository;

import com.bank.lms.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Query("SELECT a.status, COUNT(a) FROM LoanAccount a WHERE a.isDeleted = 0 GROUP BY a.status")
    List<Object[]> countByStatusAll();

    @Query("SELECT a.status, COUNT(a) FROM LoanAccount a WHERE a.isDeleted = 0 AND a.branchCode = :branchCode GROUP BY a.status")
    List<Object[]> countByStatusForBranchCode(@Param("branchCode") String branchCode);

    @Query("SELECT a.status, COUNT(a) FROM LoanAccount a WHERE a.isDeleted = 0 AND a.branchCode IN :branchCodes GROUP BY a.status")
    List<Object[]> countByStatusForBranchCodes(@Param("branchCodes") List<String> branchCodes);

    /**
     * 查询最近一次 GBase 同步时间（用于启动时判断今天是否已同步过）
     */
    @Query("SELECT MAX(a.gbaseSyncTime) FROM LoanAccount a")
    LocalDateTime findLastSyncTime();

    // ==================== 分析查询 ====================

    /** 按机构+状态聚合排名 */
    @Query("SELECT a.branchCode, a.branchName, COUNT(a), SUM(a.totalOverdueAmount) " +
           "FROM LoanAccount a WHERE a.status IN ('uncollected','collecting') AND a.isDeleted = 0 " +
           "AND a.branchCode IN :branchCodes GROUP BY a.branchCode, a.branchName " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> rankingByBranchCodes(@Param("branchCodes") List<String> branchCodes);

    /** 逾期账龄分布 */
    @Query("SELECT CASE WHEN a.overdueDays BETWEEN 1 AND 7 THEN '1-7天' " +
           "WHEN a.overdueDays BETWEEN 8 AND 30 THEN '8-30天' " +
           "WHEN a.overdueDays BETWEEN 31 AND 60 THEN '31-60天' " +
           "WHEN a.overdueDays > 60 THEN '60天+' ELSE '未知' END, COUNT(a) " +
           "FROM LoanAccount a WHERE a.status IN ('uncollected','collecting') AND a.isDeleted = 0 " +
           "AND a.branchCode IN :branchCodes GROUP BY CASE WHEN a.overdueDays BETWEEN 1 AND 7 THEN '1-7天' " +
           "WHEN a.overdueDays BETWEEN 8 AND 30 THEN '8-30天' " +
           "WHEN a.overdueDays BETWEEN 31 AND 60 THEN '31-60天' " +
           "WHEN a.overdueDays > 60 THEN '60天+' ELSE '未知' END ORDER BY MIN(a.overdueDays)")
    List<Object[]> agingDistribution(@Param("branchCodes") List<String> branchCodes);

    /** 逾期>30天积压统计 */
    @Query("SELECT a.branchCode, a.branchName, COUNT(a), SUM(a.totalOverdueAmount) " +
           "FROM LoanAccount a WHERE a.overdueDays > 30 AND a.status IN ('uncollected','collecting') " +
           "AND a.isDeleted = 0 AND a.branchCode IN :branchCodes " +
           "GROUP BY a.branchCode, a.branchName ORDER BY COUNT(a) DESC")
    List<Object[]> deepOverdueCount(@Param("branchCodes") List<String> branchCodes);

    /** 近N天每日新增逾期（按 overdue_date = createdAt 近似） */
    @Query("SELECT CAST(a.createdAt AS java.time.LocalDate), COUNT(a), SUM(a.totalOverdueAmount) " +
           "FROM LoanAccount a WHERE a.overdueDays > 0 AND a.isDeleted = 0 " +
           "AND a.branchCode IN :branchCodes AND a.createdAt >= :since " +
           "GROUP BY CAST(a.createdAt AS java.time.LocalDate) ORDER BY CAST(a.createdAt AS java.time.LocalDate)")
    List<Object[]> newOverdueDailySince(@Param("branchCodes") List<String> branchCodes,
                                         @Param("since") LocalDateTime since);
}
