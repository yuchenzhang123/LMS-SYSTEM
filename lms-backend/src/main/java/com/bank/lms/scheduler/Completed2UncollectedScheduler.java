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
        log.info("已完成->未催收定时任务已废弃，现在逾期判断由GRACE_PERIOD字段处理");
        // 此任务已不再使用，逾期判断由GBase同步时的GRACE_PERIOD变化处理
    }
}
