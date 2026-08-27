package com.bank.lms.repository;

import com.bank.lms.entity.CollectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 催收记录Repository
 */
@Repository
public interface CollectionRecordRepository extends JpaRepository<CollectionRecord, String>, JpaSpecificationExecutor<CollectionRecord> {

    List<CollectionRecord> findByLoanAccountOrderByOperateTimeDesc(String loanAccount);

    List<CollectionRecord> findByCustomerIdOrderByOperateTimeDesc(String customerId);

    List<CollectionRecord> findByMethod(String method);

    List<CollectionRecord> findByLoanAccountInOrderByOperateTimeDesc(List<String> loanAccounts);

    // ==================== 分析查询 ====================

    /** 员工工作量统计（按 operator_id 聚合，限定 branchCode 范围） */
    @Query("SELECT cr.operatorId, cr.operatorName, COUNT(cr), COUNT(DISTINCT cr.loanAccount) " +
           "FROM CollectionRecord cr JOIN LoanAccount la ON cr.loanAccount = la.loanAccount " +
           "WHERE la.branchCode IN :branchCodes AND cr.operateTime IS NOT NULL " +
           "GROUP BY cr.operatorId, cr.operatorName ORDER BY COUNT(cr) DESC")
    List<Object[]> workloadStats(@Param("branchCodes") List<String> branchCodes);

    /** 单账户催收记录按方法聚合统计 */
    @Query("SELECT cr.method, COUNT(cr), MAX(cr.result) FROM CollectionRecord cr " +
           "WHERE cr.loanAccount = :loanAccount GROUP BY cr.method")
    List<Object[]> summaryByLoanAccount(@Param("loanAccount") String loanAccount);

    /** 单账户全部催收记录（按时间倒序） */
    @Query("SELECT cr FROM CollectionRecord cr WHERE cr.loanAccount = :loanAccount " +
           "ORDER BY cr.operateTime DESC")
    List<CollectionRecord> findAllByLoanAccount(@Param("loanAccount") String loanAccount);

    /** 人员催收数量统计：按 operator（=ehrNo）聚合，限定 branchCode 范围 */
    @Query("SELECT cr.operatorId, COUNT(cr) " +
           "FROM CollectionRecord cr JOIN LoanAccount la ON cr.loanAccount = la.loanAccount " +
           "WHERE la.branchCode IN :branchCodes AND cr.operatorId IN :ehrNos " +
           "GROUP BY cr.operatorId")
    List<Object[]> operatorCollectionCount(@Param("branchCodes") List<String> branchCodes,
                                           @Param("ehrNos") List<String> ehrNos);

    /** 人员催收数量统计（限定时间范围，start/end 恒非 null） */
    @Query("SELECT cr.operatorId, COUNT(cr) " +
           "FROM CollectionRecord cr JOIN LoanAccount la ON cr.loanAccount = la.loanAccount " +
           "WHERE la.branchCode IN :branchCodes AND cr.operatorId IN :ehrNos " +
           "AND cr.operateTime >= :start AND cr.operateTime <= :end " +
           "GROUP BY cr.operatorId")
    List<Object[]> operatorCollectionCountBetween(@Param("branchCodes") List<String> branchCodes,
                                                  @Param("ehrNos") List<String> ehrNos,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);
}
