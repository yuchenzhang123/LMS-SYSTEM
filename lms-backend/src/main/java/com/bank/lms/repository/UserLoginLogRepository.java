package com.bank.lms.repository;

import com.bank.lms.entity.UserLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserLoginLogRepository extends JpaRepository<UserLoginLog, Long> {

    /** 统计某员工在一定时间内的登录次数 */
    @Query("SELECT COUNT(l) FROM UserLoginLog l WHERE l.ehrNo = :ehrNo AND l.loginTime >= :since")
    long countByEhrNoSince(@Param("ehrNo") String ehrNo, @Param("since") LocalDateTime since);

    /** 统计各员工在指定时间内的登录天数 */
    @Query("SELECT l.ehrNo, l.userName, COUNT(DISTINCT CAST(l.loginTime AS java.time.LocalDate)) " +
           "FROM UserLoginLog l WHERE l.orgCode IN :orgCodes AND l.loginTime >= :since " +
           "GROUP BY l.ehrNo, l.userName")
    List<Object[]> countActiveDaysByOrgCodeInSince(@Param("orgCodes") List<String> orgCodes,
                                                    @Param("since") LocalDateTime since);

    /** 某机构在指定时间内的活跃人数（至少登录1次） */
    @Query("SELECT COUNT(DISTINCT l.ehrNo) FROM UserLoginLog l " +
           "WHERE l.orgCode IN :orgCodes AND l.loginTime >= :since")
    long countDistinctEhrNoByOrgCodeInSince(@Param("orgCodes") List<String> orgCodes,
                                             @Param("since") LocalDateTime since);

    /** 查询最近未登录的员工 */
    @Query("SELECT u.ehrNo, u.userName, u.orgCode FROM UserOrg u " +
           "WHERE u.orgCode IN :orgCodes AND u.status = 'active' " +
           "AND u.ehrNo NOT IN (SELECT DISTINCT l.ehrNo FROM UserLoginLog l WHERE l.loginTime >= :since)")
    List<Object[]> findInactiveUsersSince(@Param("orgCodes") List<String> orgCodes,
                                           @Param("since") LocalDateTime since);

    /** 查询指定员工列表中最晚登录时间 */
    @Query("SELECT l.ehrNo, MAX(l.loginTime) FROM UserLoginLog l " +
           "WHERE l.ehrNo IN :ehrNos GROUP BY l.ehrNo")
    List<Object[]> findLastLoginByEhrNoIn(@Param("ehrNos") List<String> ehrNos);
}
