package com.bank.lms.service;

import com.bank.lms.entity.UserOrg;
import com.bank.lms.repository.UserOrgRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用户-机构数据同步服务
 * 从 GBase HR 员工视图（G_V_O_C_HRM_TBL_EMPLOYEE_INFO_U）同步到本地 user_org 表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrgSyncService {

    @Qualifier("gbaseJdbcTemplate")
    private final JdbcTemplate gbaseJdbcTemplate;
    private final UserOrgRepository userOrgRepository;

    private final AtomicBoolean syncing = new AtomicBoolean(false);

    @Transactional
    public void syncFromGbase() {
        if (!syncing.compareAndSet(false, true)) {
            log.info("用户数据同步正在进行中，跳过本次执行");
            return;
        }
        try {
            log.info("开始从 GBase 同步用户数据...");

            // 1. 取最新可用的 DATE_ID
            String maxDateSql = "SELECT MAX(DATE_ID) FROM GDM.G_V_O_C_HRM_TBL_EMPLOYEE_INFO_U";
            String dataDate = gbaseJdbcTemplate.queryForObject(maxDateSql, String.class);
            if (dataDate == null || dataDate.trim().isEmpty()) {
                log.warn("HR 员工表无数据，跳过用户同步");
                return;
            }
            log.info("使用最新可用数据日期: {}", dataDate);

            // 2. 按最新日期查询在职员工及其实际工作单位
            String sql = "SELECT A.EMPE_REFNO, A.NAME, A.ACT_EMP_ORG_REFNO, " +
                         "       B3.SIXTH_ORG_NM, A.EMPE_STS, A.OTJ_STS " +
                         "FROM GDM.G_V_O_C_HRM_TBL_EMPLOYEE_INFO_U A " +
                         "LEFT JOIN GDM.G_M0_ORG_EXTEND B3 ON A.ACT_EMP_ORG_REFNO = B3.SIXTH_ORG_ID " +
                         "WHERE A.DATE_ID = ?";
            List<Map<String, Object>> rows = gbaseJdbcTemplate.queryForList(sql, dataDate);
            log.info("从 GBase 获取 {} 条用户记录（DATE_ID={}）", rows.size(), dataDate);

            int inserted = 0, updated = 0;
            for (Map<String, Object> row : rows) {
                String ehrNo = String.valueOf(row.get("EMPE_REFNO")).trim();
                String userName = row.get("NAME") != null ? String.valueOf(row.get("NAME")).trim() : "";
                String orgCode = row.get("ACT_EMP_ORG_REFNO") != null ? String.valueOf(row.get("ACT_EMP_ORG_REFNO")).trim() : "";
                String orgName = row.get("SIXTH_ORG_NM") != null ? String.valueOf(row.get("SIXTH_ORG_NM")).trim() : "";

                if (ehrNo.isEmpty() || "null".equalsIgnoreCase(ehrNo)) continue;

                Optional<UserOrg> existing = userOrgRepository.findByEhrNo(ehrNo);
                if (existing.isPresent()) {
                    UserOrg u = existing.get();
                    boolean changed = false;
                    if (!u.getUserName().equals(userName)) { u.setUserName(userName); changed = true; }
                    if (!u.getOrgCode().equals(orgCode)) { u.setOrgCode(orgCode); changed = true; }
                    if (orgName != null && !orgName.equals(u.getOrgName())) { u.setOrgName(orgName); changed = true; }
                    if (changed || !"active".equals(u.getStatus())) {
                        u.setStatus("active");
                        u.setGbaseSyncTime(LocalDateTime.now());
                        userOrgRepository.save(u);
                        updated++;
                    }
                } else {
                    UserOrg u = new UserOrg();
                    u.setEhrNo(ehrNo);
                    u.setUserName(userName);
                    u.setOrgCode(orgCode);
                    u.setOrgName(orgName);
                    u.setStatus("active");
                    u.setGbaseSyncTime(LocalDateTime.now());
                    userOrgRepository.save(u);
                    inserted++;
                }
            }
            log.info("用户数据同步完成：新增 {} 人，更新 {} 人", inserted, updated);
        } catch (Exception e) {
            log.error("用户数据同步失败", e);
            throw new RuntimeException("用户数据同步失败", e);
        } finally {
            syncing.set(false);
        }
    }

    public boolean isSyncing() {
        return syncing.get();
    }
}
