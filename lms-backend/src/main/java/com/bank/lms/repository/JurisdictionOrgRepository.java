package com.bank.lms.repository;

import com.bank.lms.entity.JurisdictionOrg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JurisdictionOrgRepository extends JpaRepository<JurisdictionOrg, Long> {

    Optional<JurisdictionOrg> findByOrgCode(String orgCode);

    boolean existsByOrgCode(String orgCode);
}
