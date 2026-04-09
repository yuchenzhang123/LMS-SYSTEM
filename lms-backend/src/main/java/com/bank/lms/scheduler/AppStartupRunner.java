package com.bank.lms.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动后立即执行一次更新数据库的定时任务
 * 执行顺序：GBase数据同步 → 催收中→已完成状态变更
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppStartupRunner implements ApplicationRunner {

    private final GbaseDailySyncScheduler gbaseDailySyncScheduler;
    private final Collecting2CompletedScheduler collecting2CompletedScheduler;

    @Override
    public void run(ApplicationArguments args) {
        log.info("===== 应用启动初始化：开始执行定时任务 =====");
        gbaseDailySyncScheduler.execute();
        collecting2CompletedScheduler.execute();
        log.info("===== 应用启动初始化：定时任务执行完毕 =====");
    }
}
