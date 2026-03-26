package com.bank.lms.service;

import com.bank.lms.entity.LoanAccount;
import com.bank.lms.repository.LoanAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GBase 数据同步服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GbaseSyncService {

    private final JdbcTemplate jdbcTemplate;
    private final LoanAccountRepository loanAccountRepository;
    private final LoanAccountService loanAccountService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gbase.sync.view-name:gbase_loan_account_view}")
    private String gbaseViewName;

    @Transactional
    public void syncFromGbase() {
        log.info("开始执行GBase数据同步任务，视图：{}", gbaseViewName);
        try {
            String sql = "SELECT LOAN_ACCT_NO, CUST_NO, CUST_NAME, LOAN_UP_ORG_NAME, MOBILE_NO, LOAN_TYPE, DUE_STRT_DATE, LOAN_TRM, UNPD_DAYS, UNPD_TIMES, APP_AMT, LOAN_BAL, THEO_LOAN_BAL, UNPD_PRIN_BAL, CAP_UNPD_INT, UNPD_ARRS_INT_BAL, UNPD_CAP_ARRS_INT, AUTO_RISK_GRADE, GRACE_PERIOD FROM " + gbaseViewName;
            List<LoanAccount> sourceAccounts = jdbcTemplate.query(sql, new GbaseLoanAccountRowMapper());
            int total = sourceAccounts.size();
            int inserted = 0;
            int updated = 0;

            for (LoanAccount source : sourceAccounts) {
                LoanAccount existing = loanAccountRepository.findById(source.getLoanAccount()).orElse(null);
                if (existing == null) {
                    source.setStatus(source.getStatus() == null || source.getStatus().isEmpty() ? "uncollected" : source.getStatus());
                    source.setStatusUpdateTime(LocalDateTime.now());
                    source.setGbaseSyncTime(LocalDateTime.now());
                    try {
                        source.setGbaseRawData(objectMapper.writeValueAsString(source));
                    } catch (Exception e) {
                        source.setGbaseRawData(null);
                    }
                    loanAccountRepository.save(source);
                    inserted++;
                } else {
                    boolean changed = false;
                    if (source.getCustomerId() != null && !source.getCustomerId().equals(existing.getCustomerId())) {
                        existing.setCustomerId(source.getCustomerId()); changed = true;
                    }
                    if (source.getCustomerName() != null && !source.getCustomerName().equals(existing.getCustomerName())) {
                        existing.setCustomerName(source.getCustomerName()); changed = true;
                    }
                    if (source.getOrgName() != null && !source.getOrgName().equals(existing.getOrgName())) {
                        existing.setOrgName(source.getOrgName()); changed = true;
                    }
                    if (source.getPhone() != null && !source.getPhone().equals(existing.getPhone())) {
                        existing.setPhone(source.getPhone()); changed = true;
                    }
                    if (source.getProductCode() != null && !source.getProductCode().equals(existing.getProductCode())) {
                        existing.setProductCode(source.getProductCode()); changed = true;
                    }
                    if (source.getProductName() != null && !source.getProductName().equals(existing.getProductName())) {
                        existing.setProductName(source.getProductName()); changed = true;
                    }
                    if (source.getLoanDate() != null && !source.getLoanDate().equals(existing.getLoanDate())) {
                        existing.setLoanDate(source.getLoanDate()); changed = true;
                    }
                    if (source.getLoanTerm() != null && !source.getLoanTerm().equals(existing.getLoanTerm())) {
                        existing.setLoanTerm(source.getLoanTerm()); changed = true;
                    }
                    Integer oldOverdueDays = existing.getOverdueDays();
                    String oldStatus = existing.getStatus();
                    Integer oldGracePeriod = getGracePeriodFromExtraData(existing.getExtraData());

                    if (source.getOverdueDays() != null && !source.getOverdueDays().equals(existing.getOverdueDays())) {
                        existing.setOverdueDays(source.getOverdueDays()); changed = true;
                    }
                    if (source.getOverdueTimes() != null && !source.getOverdueTimes().equals(existing.getOverdueTimes())) {
                        existing.setOverdueTimes(source.getOverdueTimes()); changed = true;
                    }
                    if (source.getExpectedDays() != null && !source.getExpectedDays().equals(existing.getExpectedDays())) {
                        existing.setExpectedDays(source.getExpectedDays()); changed = true;
                    }

                    // 处理GRACE_PERIOD状态变化
                    Integer newGracePeriod = getGracePeriodFromExtraData(source.getExtraData());

                    // GRACE_PERIOD从1变为0：处理中转已完成
                    if ((oldGracePeriod != null && oldGracePeriod == 1) && (newGracePeriod != null && newGracePeriod == 0)) {
                        if ("collecting".equalsIgnoreCase(existing.getStatus())) {
                            existing.setStatus("completed");
                            existing.setStatusUpdateTime(LocalDateTime.now());
                            changed = true;
                            loanAccountService.notifyCollectingCompleted(existing);
                        }
                    }

                    // GRACE_PERIOD从0变为1：已完成转未处理
                    if ((oldGracePeriod == null || oldGracePeriod == 0) && newGracePeriod != null && newGracePeriod == 1) {
                        if ("completed".equalsIgnoreCase(existing.getStatus())) {
                            // 已完成转未处理
                            existing.setStatus("uncollected");
                            existing.setStatusUpdateTime(LocalDateTime.now());
                            changed = true;
                        }
                        // 通知新增逾期（不再自动创建催收记录）
                        loanAccountService.notifyNewOverdue(existing, source.getOverdueDays() != null ? source.getOverdueDays() : 0);
                    }
                    if (source.getContractAmount() != null && !source.getContractAmount().equals(existing.getContractAmount())) {
                        existing.setContractAmount(source.getContractAmount()); changed = true;
                    }
                    if (source.getLoanBalance() != null && !source.getLoanBalance().equals(existing.getLoanBalance())) {
                        existing.setLoanBalance(source.getLoanBalance()); changed = true;
                    }
                    if (source.getUnexpiredPrincipal() != null && !source.getUnexpiredPrincipal().equals(existing.getUnexpiredPrincipal())) {
                        existing.setUnexpiredPrincipal(source.getUnexpiredPrincipal()); changed = true;
                    }
                    if (source.getOverduePrincipal() != null && !source.getOverduePrincipal().equals(existing.getOverduePrincipal())) {
                        existing.setOverduePrincipal(source.getOverduePrincipal()); changed = true;
                    }
                    if (source.getOverdueInterest() != null && !source.getOverdueInterest().equals(existing.getOverdueInterest())) {
                        existing.setOverdueInterest(source.getOverdueInterest()); changed = true;
                    }
                    if (source.getOverduePenalty() != null && !source.getOverduePenalty().equals(existing.getOverduePenalty())) {
                        existing.setOverduePenalty(source.getOverduePenalty()); changed = true;
                    }
                    if (source.getTotalOverdueAmount() != null && !source.getTotalOverdueAmount().equals(existing.getTotalOverdueAmount())) {
                        existing.setTotalOverdueAmount(source.getTotalOverdueAmount()); changed = true;
                    }
                    existing.setGbaseSyncTime(LocalDateTime.now());
                    try {
                        existing.setGbaseRawData(objectMapper.writeValueAsString(source));
                    } catch (Exception e) {
                        // ignore
                    }
                    if (changed) {
                        loanAccountRepository.save(existing);
                        updated++;
                    }
                }
            }

            log.info("GBase数据同步完成：总={}，新增={}，更新={}", total, inserted, updated);

            // 同步后做状态转换
            int collectToCompleted = loanAccountService.moveCollectingToCompletedByExpectedDaysZero();
            log.info("同步后状态处理完成：催收中->已完成={}", collectToCompleted);
        } catch (Exception e) {
            log.error("GBase数据同步失败", e);
            throw new RuntimeException("GBase数据同步失败", e);
        }
    }

    private static class GbaseLoanAccountRowMapper implements RowMapper<LoanAccount> {
        @Override
        public LoanAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
            LoanAccount account = new LoanAccount();
            account.setLoanAccount(rs.getString("LOAN_ACCT_NO"));
            account.setCustomerId(rs.getString("CUST_NO"));
            account.setCustomerName(rs.getString("CUST_NAME"));
            account.setOrgName(rs.getString("LOAN_UP_ORG_NAME"));
            account.setPhone(rs.getString("MOBILE_NO"));
            account.setProductCode(rs.getString("LOAN_TYPE"));
            account.setLoanDate(rs.getDate("DUE_STRT_DATE") != null ? rs.getDate("DUE_STRT_DATE").toLocalDate() : null);
            account.setLoanTerm(rs.getObject("LOAN_TRM") != null ? rs.getInt("LOAN_TRM") : null);
            account.setOverdueDays(rs.getObject("UNPD_DAYS") != null ? rs.getInt("UNPD_DAYS") : 0);
            account.setOverdueTimes(rs.getObject("UNPD_TIMES") != null ? rs.getInt("UNPD_TIMES") : 0);
            account.setContractAmount(rs.getBigDecimal("APP_AMT"));
            account.setLoanBalance(rs.getBigDecimal("LOAN_BAL"));
            account.setUnexpiredPrincipal(rs.getBigDecimal("THEO_LOAN_BAL"));
            account.setOverduePrincipal(rs.getBigDecimal("UNPD_PRIN_BAL"));
            account.setOverdueInterest(rs.getBigDecimal("CAP_UNPD_INT"));
            account.setOverduePenalty(rs.getBigDecimal("UNPD_ARRS_INT_BAL"));
            account.setTotalOverdueAmount(rs.getBigDecimal("UNPD_CAP_ARRS_INT"));

            // 根据GRACE_PERIOD判断是否逾期：0-未逾期，1-逾期
            Integer gracePeriod = rs.getObject("GRACE_PERIOD") != null ? rs.getInt("GRACE_PERIOD") : 0;
            account.setStatus(convertSourceStatus(gracePeriod));

            account.setExpectedDays(0); // 视业务确定，可从其他字段或逻辑计算
            account.setStatusUpdateTime(LocalDateTime.now());
            
            Map<String, Object> extra = new HashMap<>();
            extra.put("autoRiskGrade", rs.getString("AUTO_RISK_GRADE"));
            extra.put("gracePeriod", gracePeriod);
            try {
                account.setExtraData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(extra));
            } catch (Exception ignore) {
                account.setExtraData(null);
            }

            return account;
        }

        private String convertSourceStatus(Integer gracePeriod) {
            // 根据GRACE_PERIOD判断初始状态
            // GRACE_PERIOD = 0：宽限期内，未逾期 -> uncollected
            // GRACE_PERIOD = 1：宽限期结束，已逾期 -> collecting
            if (gracePeriod != null && gracePeriod == 1) {
                return "collecting"; // 已逾期，需要处理
            }
            return "uncollected"; // 未逾期，宽限期内
        }

        private Integer getGracePeriodFromExtraData(String extraData) {
            if (extraData == null || extraData.trim().isEmpty()) {
                return 0;
            }
            try {
                Map<String, Object> extra = objectMapper.readValue(extraData, Map.class);
                Object gracePeriod = extra.get("gracePeriod");
                if (gracePeriod instanceof Integer) {
                    return (Integer) gracePeriod;
                } else if (gracePeriod instanceof Number) {
                    return ((Number) gracePeriod).intValue();
                }
            } catch (Exception e) {
                // ignore
            }
            return 0;
        }
    }
}
