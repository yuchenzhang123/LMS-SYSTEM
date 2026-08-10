package com.bank.lms.service.analysis;

import com.bank.lms.entity.CollectionRecord;
import com.bank.lms.entity.LoanAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 账户催收优先级评分（纯算法，无需 LLM）
 * 综合逾期天数、金额、最近催收响应情况三个维度
 */
@Slf4j
@Component
public class AccountPriorityScorer {

    /**
     * 计算账户催收优先级分数（0-100）
     */
    public double score(LoanAccount account, CollectionRecord latestRecord) {
        if (account == null) return 0;

        // 1. 逾期天数分 (0-40)
        double daysScore = 0;
        if (account.getOverdueDays() != null) {
            daysScore = Math.min(account.getOverdueDays() / 90.0 * 40, 40);
        }

        // 2. 金额分 (0-30)，按总逾期金额的对数缩放
        double amtScore = 0;
        BigDecimal amt = account.getTotalOverdueAmount();
        if (amt != null && amt.doubleValue() > 0) {
            amtScore = Math.min(Math.log10(amt.doubleValue() + 1) / 6.0 * 30, 30);
        }

        // 3. 最近响应分 (0-30)：越久无响应分越高
        double responseScore = 15; // 默认中等
        if (latestRecord != null && latestRecord.getOperateTime() != null) {
            long daysSinceLast = ChronoUnit.DAYS.between(latestRecord.getOperateTime(), LocalDateTime.now());
            if (daysSinceLast > 14) {
                responseScore = 30;
            } else if (daysSinceLast > 7) {
                responseScore = 22;
            } else if (daysSinceLast <= 2) {
                responseScore = 8;
            }
        } else {
            // 从未催收过，最高优先级
            responseScore = 30;
        }

        return daysScore + amtScore + responseScore;
    }
}
