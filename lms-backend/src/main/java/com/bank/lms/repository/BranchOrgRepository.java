package com.bank.lms.repository;

import com.bank.lms.entity.BranchOrg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchOrgRepository extends JpaRepository<BranchOrg, Long> {

    List<BranchOrg> findByOrgCode(String orgCode);

    /** 批量查询，修复 getOrgTree N+1 */
    List<BranchOrg> findByOrgCodeIn(List<String> orgCodes);

    List<BranchOrg> findByBranchCode(String branchCode);

    /** 删除分支行在指定管辖行下的记录 */
    void deleteByBranchCodeAndOrgCode(String branchCode, String orgCode);

    boolean existsByBranchCode(String branchCode);

    boolean existsByBranchCodeAndOrgCode(String branchCode, String orgCode);
}
