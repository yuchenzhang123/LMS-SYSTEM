package com.bank.lms.service;

import com.bank.lms.dto.request.AccountQueryRequest;
import com.bank.lms.dto.response.AccountDetailResponse;
import com.bank.lms.entity.LoanAccount;
import com.bank.lms.repository.LoanAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 贷款账户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanAccountService {

    private final LoanAccountRepository loanAccountRepository;
    private final NoticeService noticeService;

    /**
     * 查询账户列表
     */
    public Map<String, Object> getAccountList(AccountQueryRequest request) {
        Specification<LoanAccount> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getCustomerId() != null && !request.getCustomerId().trim().isEmpty()) {
                predicates.add(cb.like(root.get("customerId"), "%" + request.getCustomerId().trim() + "%"));
            }
            if (request.getLoanAccount() != null && !request.getLoanAccount().trim().isEmpty()) {
                predicates.add(cb.like(root.get("loanAccount"), "%" + request.getLoanAccount().trim() + "%"));
            }
            if (request.getProductCode() != null && !request.getProductCode().trim().isEmpty()) {
                predicates.add(cb.like(root.get("productCode"), "%" + request.getProductCode().trim() + "%"));
            }
            if (request.getOverdueDays() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("overdueDays"), request.getOverdueDays()));
            }
            if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), request.getStatus().trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int current = request.getPage() != null ? request.getPage().getCurrentPage() : 1;
        int size = request.getPage() != null ? request.getPage().getPageSize() : 10;

        Page<LoanAccount> page = loanAccountRepository.findAll(spec,
                PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Map<String, Object>> records = page.getContent().stream()
                .map(this::toListItem)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", page.getTotalElements());
        result.put("size", size);
        result.put("current", current);

        return result;
    }

    /**
     * 未催收->催收中，如果当前状态为uncollected则更新为collecting
     */
    @Transactional
    public void markCollectingIfUncollected(String loanAccount) {
        LoanAccount account = loanAccountRepository.findById(loanAccount)
                .orElseThrow(() -> new RuntimeException("未查询到账户信息"));
        if ("uncollected".equalsIgnoreCase(account.getStatus())) {
            account.setStatus("collecting");
            account.setStatusUpdateTime(java.time.LocalDateTime.now());
            loanAccountRepository.save(account);
            log.info("账户状态由未催收变为催收中: {}", loanAccount);
        }
    }

    /**
     * 催收中->已完成：预期天数为0
     */
    @Transactional
    public int moveCollectingToCompletedByExpectedDaysZero() {
        List<LoanAccount> accounts = loanAccountRepository.findByStatus("collecting");
        int changed = 0;
        for (LoanAccount account : accounts) {
            if (account.getExpectedDays() != null && account.getExpectedDays() == 0) {
                account.setStatus("completed");
                account.setStatusUpdateTime(java.time.LocalDateTime.now());
                loanAccountRepository.save(account);

                String title = "逾期催收已完成还款";
                String message = String.format("贷款账号 %s 客户 %s 逾期 %d 天已完成还款，已转为已完成状态。", account.getLoanAccount(), account.getCustomerName(), account.getOverdueDays() == null ? 0 : account.getOverdueDays());
                noticeService.createNotice(title, "high", message,
                        account.getCustomerId(), account.getLoanAccount(), account.getCustomerName(), account.getProductCode(), account.getOverdueDays());

                changed++;
                log.info("账户状态由催收中变更已完成: {}", account.getLoanAccount());
            }
        }
        return changed;
    }

    /**
     * 已完成->未催收：逾期天数从0变为非0
     */
    @Transactional
    public int moveCompletedToUncollectedByOverdueDaysPositive() {
        List<LoanAccount> accounts = loanAccountRepository.findByStatus("completed");
        int changed = 0;
        for (LoanAccount account : accounts) {
            if (account.getOverdueDays() != null && account.getOverdueDays() > 0) {
                int oldOverdue = account.getOverdueDays();
                account.setStatus("uncollected");
                account.setStatusUpdateTime(java.time.LocalDateTime.now());
                loanAccountRepository.save(account);

                String title = "新增逾期通知";
                String message = String.format("贷款账号 %s 客户 %s 逾期天数由0变为 %d，请及时跟进。", account.getLoanAccount(), account.getCustomerName(), oldOverdue);
                noticeService.createNotice(title, "high", message,
                        account.getCustomerId(), account.getLoanAccount(), account.getCustomerName(), account.getProductCode(), oldOverdue);

                changed++;
                log.info("账户状态由已完成变更未催收: {}", account.getLoanAccount());
            }
        }
        return changed;
    }

    /**
     * 获取账户详情
     */
    public AccountDetailResponse getAccountDetail(String loanAccount) {
        LoanAccount account = loanAccountRepository.findById(loanAccount)
                .orElseThrow(() -> new RuntimeException("未查询到账户信息"));

        AccountDetailResponse response = new AccountDetailResponse();
        response.setLoanAccount(account.getLoanAccount());
        response.setCustomerId(account.getCustomerId());
        response.setCustomerName(account.getCustomerName());
        response.setOrgName(account.getOrgName());
        response.setPhone(account.getPhone());
        response.setProductCode(account.getProductCode());
        response.setProductName(account.getProductName());
        response.setLoanDate(account.getLoanDate() != null ? account.getLoanDate().toString() : null);
        response.setLoanTerm(account.getLoanTerm());
        response.setOverdueDays(account.getOverdueDays());
        response.setContractAmount(formatAmount(account.getContractAmount()));
        response.setLoanBalance(formatAmount(account.getLoanBalance()));
        response.setUnexpiredPrincipal(formatAmount(account.getUnexpiredPrincipal()));
        response.setOverduePrincipal(formatAmount(account.getOverduePrincipal()));
        response.setOverdueInterest(formatAmount(account.getOverdueInterest()));
        response.setOverduePenalty(formatAmount(account.getOverduePenalty()));
        response.setTotalOverdueAmount(formatAmount(account.getTotalOverdueAmount()));
        response.setStatus(account.getStatus());

        return response;
    }

    private Map<String, Object> toListItem(LoanAccount account) {
        Map<String, Object> item = new HashMap<>();
        item.put("customerId", account.getCustomerId());
        item.put("loanAccount", account.getLoanAccount());
        item.put("customerName", account.getCustomerName());
        item.put("productCode", account.getProductCode());
        item.put("overdueDays", account.getOverdueDays());
        item.put("status", account.getStatus());
        return item;
    }

    public void notifyNewOverdue(LoanAccount account, int newOverdueDays) {
        String title = "新增逾期通知";
        String message = String.format("贷款账号 %s 客户 %s 逾期天数由 0 变为 %d，请及时跟进。", account.getLoanAccount(), account.getCustomerName(), newOverdueDays);
        noticeService.createNotice(title, "high", message,
                account.getCustomerId(), account.getLoanAccount(), account.getCustomerName(), account.getProductCode(), newOverdueDays);
    }

    public void notifyCollectingCompleted(LoanAccount account) {
        String title = "逾期催收已完成还款";
        String message = String.format("贷款账号 %s 客户 %s 逾期 %d 天已完成还款，已转为已完成状态。", account.getLoanAccount(), account.getCustomerName(), account.getOverdueDays() == null ? 0 : account.getOverdueDays());
        noticeService.createNotice(title, "high", message,
                account.getCustomerId(), account.getLoanAccount(), account.getCustomerName(), account.getProductCode(), account.getOverdueDays());
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}
