package com.bank.lms.scheduler;

import com.bank.lms.service.LoanAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 已完成->未催收定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Completed2UncollectedScheduler {

    private final LoanAccountService loanAccountService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void execute() {
        log.info("开始执行已完成->未催收定时任务");
        try {
            int changed = loanAccountService.moveCompletedToUncollectedByOverdueDaysPositive();
            log.info("已完成->未催收状态变更完成，变更数量={}。", changed);
        } catch (Exception e) {
            log.error("已完成->未催收定时任务执行失败", e);
        }
    }
}
