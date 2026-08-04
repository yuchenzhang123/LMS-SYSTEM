package com.bank.lms.repository;

import com.bank.lms.entity.OrgGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrgGroupMemberRepository extends JpaRepository<OrgGroupMember, Long> {

    List<OrgGroupMember> findByGroupCode(String groupCode);

    List<OrgGroupMember> findByOrgCode(String orgCode);

    Optional<OrgGroupMember> findByGroupCodeAndOrgCode(String groupCode, String orgCode);

    boolean existsByGroupCodeAndOrgCode(String groupCode, String orgCode);

    boolean existsByOrgCode(String orgCode);

    void deleteByGroupCodeAndOrgCode(String groupCode, String orgCode);

    void deleteByGroupCode(String groupCode);
}
