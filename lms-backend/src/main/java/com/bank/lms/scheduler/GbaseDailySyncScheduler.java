package com.bank.lms.scheduler;

import com.bank.lms.service.GbaseSyncService;
import com.bank.lms.service.analysis.CopilotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;

/**
 * 每日同步 GBase 视图数据
 * 主触发时间 + 兜底重试（防止 GBase 灌数延迟导致空同步）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GbaseDailySyncScheduler {

    private final GbaseSyncService gbaseSyncService;
    private final CopilotService copilotService;

    @Schedules({
        @Scheduled(cron = "${scheduler.cron.gbase-sync:0 0 6 * * ?}"),
        @Scheduled(cron = "${scheduler.cron.gbase-sync-retry:0 0 12 * * ?}")
    })
    public void execute() {
        log.info("【定时任务触发】开始执行GBase每日同步，线程：{}", Thread.currentThread().getName());
        try {
            gbaseSyncService.syncFromGbase();
            // 数据已更新，清空简报缓存，使下次访问重新生成最新数据
            copilotService.clearBriefingCache();
            log.info("【定时任务完成】GBase每日同步定时任务完成");
        } catch (Exception e) {
            log.error("【定时任务失败】GBase每日同步定时任务失败", e);
        }
    }
}
