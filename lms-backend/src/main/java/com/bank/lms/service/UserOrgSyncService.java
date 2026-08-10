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
 * 从 GBase R_V_O_USER_BASIC 同步用户信息到本地 user_org 表
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
            String sql = "SELECT USER_ID, USER_NAME, ORG_ID FROM rcrms.R_V_O_USER_BASIC";
            List<Map<String, Object>> rows = gbaseJdbcTemplate.queryForList(sql);
            log.info("从 GBase 获取 {} 条用户记录", rows.size());

            int inserted = 0, updated = 0;
            for (Map<String, Object> row : rows) {
                String ehrNo = String.valueOf(row.get("USER_ID")).trim();
                String userName = row.get("USER_NAME") != null ? String.valueOf(row.get("USER_NAME")).trim() : "";
                String orgCode = row.get("ORG_ID") != null ? String.valueOf(row.get("ORG_ID")).trim() : "";

                if (ehrNo.isEmpty()) continue;

                Optional<UserOrg> existing = userOrgRepository.findByEhrNo(ehrNo);
                if (existing.isPresent()) {
                    UserOrg u = existing.get();
                    boolean changed = false;
                    if (!u.getUserName().equals(userName)) { u.setUserName(userName); changed = true; }
                    if (!u.getOrgCode().equals(orgCode)) { u.setOrgCode(orgCode); changed = true; }
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
