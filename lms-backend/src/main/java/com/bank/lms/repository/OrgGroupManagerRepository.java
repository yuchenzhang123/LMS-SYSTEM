package com.bank.lms.repository;

import com.bank.lms.entity.OrgGroupManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrgGroupManagerRepository extends JpaRepository<OrgGroupManager, Long> {

    List<OrgGroupManager> findByGroupCode(String groupCode);

    /** 查找某个人员在所有组的管理人员记录 */
    List<OrgGroupManager> findByEhrNo(String ehrNo);

    Optional<OrgGroupManager> findByGroupCodeAndEhrNo(String groupCode, String ehrNo);

    boolean existsByGroupCodeAndEhrNo(String groupCode, String ehrNo);

    void deleteByGroupCodeAndEhrNo(String groupCode, String ehrNo);

    void deleteByGroupCode(String groupCode);
}
