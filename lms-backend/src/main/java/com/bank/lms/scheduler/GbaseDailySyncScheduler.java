package com.bank.lms.scheduler;

import com.bank.lms.service.GbaseSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日同步 GBase 视图数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GbaseDailySyncScheduler {

    private final GbaseSyncService gbaseSyncService;

    @Scheduled(cron = "0 0 1 * * ?")
    public void execute() {
        log.info("【定时任务触发】开始执行GBase每日同步，线程：{}", Thread.currentThread().getName());
        try {
            gbaseSyncService.syncFromGbase();
            log.info("【定时任务完成】GBase每日同步定时任务完成");
        } catch (Exception e) {
            log.error("【定时任务失败】GBase每日同步定时任务失败", e);
        }
    }
}
