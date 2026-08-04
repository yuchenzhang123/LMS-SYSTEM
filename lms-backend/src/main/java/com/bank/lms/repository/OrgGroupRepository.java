package com.bank.lms.repository;

import com.bank.lms.entity.OrgGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrgGroupRepository extends JpaRepository<OrgGroup, Long> {

    Optional<OrgGroup> findByGroupCode(String groupCode);

    boolean existsByGroupCode(String groupCode);

    void deleteByGroupCode(String groupCode);
}
