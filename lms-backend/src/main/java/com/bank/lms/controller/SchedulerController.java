package com.bank.lms.controller;

import com.bank.lms.common.Result;
import com.bank.lms.scheduler.GbaseDailySyncScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final GbaseDailySyncScheduler gbaseDailySyncScheduler;

    @PostMapping("/sync-gbase")
    public Result<String> triggerGbaseSync() {
        log.info("[手动触发] GBase 数据同步");
        gbaseDailySyncScheduler.execute();
        return Result.success("GBase 同步已执行");
    }
}
