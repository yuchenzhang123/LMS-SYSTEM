package com.bank.lms.controller;

import com.bank.lms.common.Result;
import com.bank.lms.scheduler.Collecting2CompletedScheduler;
import com.bank.lms.scheduler.GbaseDailySyncScheduler;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/scheduler")
@RequiredArgsConstructor
@Api(tags = "定时任务手动触发")
public class SchedulerController {

    private final GbaseDailySyncScheduler gbaseDailySyncScheduler;
    private final Collecting2CompletedScheduler collecting2CompletedScheduler;

    @PostMapping("/sync-gbase")
    @ApiOperation("手动触发 GBase 数据同步")
    public Result<String> triggerGbaseSync() {
        log.info("[手动触发] GBase 数据同步");
        gbaseDailySyncScheduler.execute();
        return Result.success("GBase 同步已执行");
    }

    @PostMapping("/collecting-to-completed")
    @ApiOperation("手动触发催收中→已完成状态变更")
    public Result<String> triggerCollecting2Completed() {
        log.info("[手动触发] 催收中->已完成");
        collecting2CompletedScheduler.execute();
        return Result.success("催收中->已完成已执行");
    }
}
