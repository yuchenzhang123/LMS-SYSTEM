package com.bank.lms.scheduler;

import com.bank.lms.service.UserOrgSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 用户数据同步定时任务（每日 6:15）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserOrgSyncScheduler {

    private final UserOrgSyncService userOrgSyncService;

    @Scheduled(cron = "${scheduler.cron.user-org-sync:0 15 6 * * ?}")
    public void execute() {
        log.info("UserOrgSyncScheduler 开始执行");
        try {
            userOrgSyncService.syncFromGbase();
        } catch (Exception e) {
            log.error("UserOrgSyncScheduler 执行失败", e);
        }
    }
}
