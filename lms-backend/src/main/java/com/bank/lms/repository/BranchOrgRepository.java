package com.bank.lms.repository;

import com.bank.lms.entity.BranchOrg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchOrgRepository extends JpaRepository<BranchOrg, Long> {

    List<BranchOrg> findByOrgCode(String orgCode);

    Optional<BranchOrg> findByBranchCode(String branchCode);

    boolean existsByBranchCode(String branchCode);

    boolean existsByBranchCodeAndOrgCode(String branchCode, String orgCode);
}
