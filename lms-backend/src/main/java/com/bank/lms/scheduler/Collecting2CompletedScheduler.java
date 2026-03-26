package com.bank.lms.scheduler;

import com.bank.lms.service.LoanAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 催收中->已完成定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Collecting2CompletedScheduler {

    private final LoanAccountService loanAccountService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void execute() {
        log.info("开始执行催收中->已完成定时任务");
        try {
            int changed = loanAccountService.moveCollectingToCompletedByExpectedDaysZero();
            log.info("催收中->已完成状态变更完成，变更数量={}。", changed);
        } catch (Exception e) {
            log.error("催收中->已完成定时任务执行失败", e);
        }
    }
}
