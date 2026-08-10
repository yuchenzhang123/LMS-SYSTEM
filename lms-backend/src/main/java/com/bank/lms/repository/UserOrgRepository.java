package com.bank.lms.repository;

import com.bank.lms.entity.UserOrg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserOrgRepository extends JpaRepository<UserOrg, Long> {

    Optional<UserOrg> findByEhrNo(String ehrNo);

    List<UserOrg> findByOrgCode(String orgCode);

    @Query("SELECT u FROM UserOrg u WHERE u.orgCode IN :orgCodes AND u.status = 'active'")
    List<UserOrg> findActiveByOrgCodeIn(@Param("orgCodes") List<String> orgCodes);

    @Query("SELECT COUNT(u) FROM UserOrg u WHERE u.orgCode IN :orgCodes AND u.status = 'active'")
    long countActiveByOrgCodeIn(@Param("orgCodes") List<String> orgCodes);

    List<UserOrg> findByOrgCodeIn(List<String> orgCodes);
}
