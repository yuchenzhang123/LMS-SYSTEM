package com.bank.lms.unit;

import com.bank.lms.entity.CollectionRecord;
import com.bank.lms.entity.LoanAccount;
import com.bank.lms.service.analysis.AccountPriorityScorer;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.bank.lms.common.TestConstants.*;
import static org.assertj.core.api.Assertions.*;

/**
 * 单元测试 — 智能优先级评分
 * 纯逻辑测试，无任何外部依赖
 */
@Label("智能优先级评分 (单元测试)")
class AccountPriorityScorerTest {

    private final AccountPriorityScorer scorer = new AccountPriorityScorer();

    // ==================== Property-Based Testing ====================

    @Property(tries = 500)
    @Label("任意合法输入得分在[0,100]")
    boolean scoreAlwaysInRange(
        @ForAll @IntRange(min = 0, max = 9999) int days,
        @ForAll @DoubleRange(min = 0, max = 1_000_000_000) double amt,
        @ForAll boolean hasRecord
    ) {
        LoanAccount a = account(days, amt);
        CollectionRecord r = hasRecord ? recentRecord() : null;
        double score = scorer.score(a, r);
        return score >= 0.0 && score <= 100.0;
    }

    @Property(tries = 300)
    @Label("逾期更久+金额更大 → 得分更高（单调性）")
    boolean monotonic(
        @ForAll @IntRange(min = 0, max = 365) int baseDays,
        @ForAll @DoubleRange(min = 100, max = 10_000_000) double baseAmt,
        @ForAll @IntRange(min = 1, max = 100) int extraDays,
        @ForAll @DoubleRange(min = 1, max = 1_000_000) double extraAmt
    ) {
        double worseScore  = scorer.score(account(baseDays + extraDays, baseAmt + extraAmt), null);
        double betterScore = scorer.score(account(baseDays, baseAmt), null);
        return worseScore >= betterScore;
    }

    @Property(tries = 300)
    @Label("从未催收 ≥ 最近催收过")
    boolean noCollectionHigherThanRecent(
        @ForAll @IntRange(min = 1, max = 365) int days,
        @ForAll @DoubleRange(min = 1000, max = 10_000_000) double amt
    ) {
        LoanAccount a = account(days, amt);
        return scorer.score(a, null) >= scorer.score(a, recentRecord());
    }

    // ==================== 边界值 ====================

    @Test @DisplayName("空账户 → 0分")
    void nullAccountReturnsZero() {
        assertThat(scorer.score(null, null)).isZero();
    }

    @Test @DisplayName("逾期0天+金额0+最近催收过 → 低分")
    void zeroEverythingWithRecord() {
        double score = scorer.score(account(OVERDUE_ZERO, AMOUNT_ZERO), recentRecord());
        assertThat(score).isLessThan(20.0); // 仅剩响应分最低档
    }

    @Test
    @DisplayName("极端逾期999天+千万金额 → >=90分")
    void extremeCaseNearFullScore() {
        double score = scorer.score(account(OVERDUE_EXTREME, AMOUNT_HUGE), null);
        assertThat(score).isGreaterThanOrEqualTo(90.0);
    }

    @Test
    @DisplayName("逾期3天+小额+刚催收 → 较低分")
    void freshAccountLowScore() {
        double score = scorer.score(account(OVERDUE_LOW, AMOUNT_SMALL), recentRecord());
        assertThat(score).isLessThan(35.0); // 逾期低+小额+刚催收
    }

    @Test
    @DisplayName("高风险 vs 低风险排序验证")
    void highRiskBeforeLowRisk() {
        LoanAccount high = account(OVERDUE_HIGH, AMOUNT_LARGE);
        LoanAccount low  = account(OVERDUE_LOW, AMOUNT_SMALL);

        assertThat(scorer.score(high, null))
            .as("高风险(逾期%d天,%.0f元)", OVERDUE_HIGH, AMOUNT_LARGE)
            .isGreaterThan(scorer.score(low, recentRecord()));
    }

    // ==================== 异常输入 ====================

    @Test @DisplayName("负逾期天数被接受不抛异常")
    void negativeDaysDoesNotThrow() {
        assertThatCode(() -> scorer.score(account(-5, AMOUNT_MEDIUM), null))
            .doesNotThrowAnyException();
    }

    @Test @DisplayName("null金额被处理不抛异常")
    void nullAmountDoesNotThrow() {
        LoanAccount a = new LoanAccount();
        a.setOverdueDays(OVERDUE_MEDIUM);
        a.setTotalOverdueAmount(null);
        assertThatCode(() -> scorer.score(a, null)).doesNotThrowAnyException();
    }

    // ==================== 数据工厂 ====================

    private LoanAccount account(int days, double amt) {
        LoanAccount a = new LoanAccount();
        a.setOverdueDays(days);
        a.setTotalOverdueAmount(amt > 0 ? BigDecimal.valueOf(amt) : BigDecimal.ZERO);
        return a;
    }

    private CollectionRecord recentRecord() {
        CollectionRecord r = new CollectionRecord();
        r.setOperateTime(LocalDateTime.now().minusHours(1));
        return r;
    }
}
