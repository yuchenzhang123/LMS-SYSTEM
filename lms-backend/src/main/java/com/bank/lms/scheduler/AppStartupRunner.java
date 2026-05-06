package com.bank.lms.scheduler;

import com.bank.lms.repository.LoanAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应用启动后检查是否需要执行数据同步：
 * - 本地表为空（首次启动）：立即执行全量导入
 * - 今天尚未同步过：立即执行增量同步
 * - 今天已同步过：跳过，等待定时任务下次执行
 * 避免与定时任务补执行产生并发冲突
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppStartupRunner implements ApplicationRunner {

    private final GbaseDailySyncScheduler gbaseDailySyncScheduler;
    private final LoanAccountRepository loanAccountRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("===== 应用启动：检查是否需要执行数据同步 =====");
        runAsync();
    }

    @Async
    public void runAsync() {
        try {
            // 本地表为空，首次启动，必须立即同步
            long localCount = loanAccountRepository.count();
            if (localCount == 0) {
                log.info("===== 本地表为空，立即执行首次全量导入 =====");
                gbaseDailySyncScheduler.execute();
                return;
            }

            // 检查今天是否已经同步过
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime lastSync = loanAccountRepository.findLastSyncTime();
            if (lastSync != null && lastSync.isAfter(todayStart)) {
                log.info("===== 今天已同步过（{}），跳过启动同步，等待定时任务 =====", lastSync);
                return;
            }

            log.info("===== 今天尚未同步，立即执行增量同步 =====");
            gbaseDailySyncScheduler.execute();

        } catch (Exception e) {
            log.error("===== 启动同步失败 =====", e);
        }
    }
}
