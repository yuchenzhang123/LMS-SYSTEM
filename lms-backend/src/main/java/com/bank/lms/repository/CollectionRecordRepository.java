package com.bank.lms.repository;

import com.bank.lms.entity.CollectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 催收记录Repository
 */
@Repository
public interface CollectionRecordRepository extends JpaRepository<CollectionRecord, String>, JpaSpecificationExecutor<CollectionRecord> {

    List<CollectionRecord> findByLoanAccountOrderByOperateTimeDesc(String loanAccount);

    List<CollectionRecord> findByCustomerIdOrderByOperateTimeDesc(String customerId);

    List<CollectionRecord> findByMethod(String method);
}
