package com.bank.lms.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动后立即执行一次 GBase 数据同步 + 兜底状态修正
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
