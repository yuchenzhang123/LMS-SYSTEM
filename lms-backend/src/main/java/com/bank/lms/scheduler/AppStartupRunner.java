package com.bank.lms.scheduler;

import com.bank.lms.repository.LoanAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动后异步执行数据同步检查：
 * - 本地表为空（首次启动）：立即执行全量导入
 * - 今天尚未同步过：立即执行增量同步
 * - 今天已同步过：跳过
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppStartupRunner implements ApplicationRunner {

    private final GbaseDailySyncScheduler gbaseDailySyncScheduler;
    private final LoanAccountRepository loanAccountRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("【启动触发】应用启动：触发后台数据同步检查");
        // 用独立线程异步执行，避免阻塞启动流程
        // 注意：不用 @Async，因为同类内部调用 @Async 不走代理，异步不生效
        Thread syncThread = new Thread(this::doSync, "startup-sync");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private void doSync() {
        log.info("【启动同步】startup-sync 线程开始执行");
        try {
            long localCount = loanAccountRepository.count();
            if (localCount == 0) {
                log.info("【启动同步】本地表为空，立即执行首次全量导入");
                gbaseDailySyncScheduler.execute();
                return;
            }

            java.time.LocalDateTime todayStart = java.time.LocalDate.now().atStartOfDay();
            java.time.LocalDateTime lastSync = loanAccountRepository.findLastSyncTime();
            if (lastSync != null && lastSync.isAfter(todayStart)) {
                log.info("【启动同步】今天已同步过（{}），跳过启动同步", lastSync);
                return;
            }

            log.info("【启动同步】今天尚未同步，立即执行增量同步");
            gbaseDailySyncScheduler.execute();

        } catch (Exception e) {
            log.error("【启动同步】启动同步失败", e);
        }
    }
}
