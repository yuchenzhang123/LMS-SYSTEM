package com.bank.lms.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 应用启动后异步执行一次 GBase 数据同步
 * 不阻塞启动过程，同步在后台线程中运行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppStartupRunner implements ApplicationRunner {

    private final GbaseDailySyncScheduler gbaseDailySyncScheduler;

    @Override
    public void run(ApplicationArguments args) {
        log.info("===== 应用启动：已触发后台数据同步，不阻塞启动流程 =====");
        runAsync();
    }

    @Async
    public void runAsync() {
        log.info("===== 后台初始化同步开始 =====");
        try {
            gbaseDailySyncScheduler.execute();
            log.info("===== 后台初始化同步完成 =====");
        } catch (Exception e) {
            log.error("===== 后台初始化同步失败 =====", e);
        }
    }
}
