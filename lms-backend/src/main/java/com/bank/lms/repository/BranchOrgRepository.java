package com.bank.lms.repository;

import com.bank.lms.entity.BranchOrg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchOrgRepository extends JpaRepository<BranchOrg, Long> {

    List<BranchOrg> findByOrgCode(String orgCode);

    List<BranchOrg> findByBranchCode(String branchCode);

    boolean existsByBranchCode(String branchCode);

    boolean existsByBranchCodeAndOrgCode(String branchCode, String orgCode);
}
