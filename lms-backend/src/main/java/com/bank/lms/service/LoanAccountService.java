package com.bank.lms.service;

import com.bank.lms.dto.request.AccountQueryRequest;
import com.bank.lms.dto.response.AccountDetailResponse;
import com.bank.lms.entity.LoanAccount;
import com.bank.lms.repository.LoanAccountRepository;
import com.bank.lms.repository.BranchOrgRepository;
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
    private final BranchOrgRepository branchOrgRepository;

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
            if (request.getBranchCode() != null && !request.getBranchCode().trim().isEmpty()) {
                // 精确匹配单个分支行
                predicates.add(cb.equal(root.get("branchCode"), request.getBranchCode().trim()));
            } else if (request.getOrgCode() != null && !request.getOrgCode().trim().isEmpty()) {
                // 管辖行范围查询：下属分支行 + 管辖行自身（管辖行本身也可能有贷款数据）
                String orgCode = request.getOrgCode().trim();
                List<String> codes = new java.util.ArrayList<>(
                        branchOrgRepository.findByOrgCode(orgCode)
                                .stream()
                                .map(b -> b.getBranchCode())
                                .collect(java.util.stream.Collectors.toList()));
                if (!codes.contains(orgCode)) {
                    codes.add(orgCode);
                }
                predicates.add(root.get("branchCode").in(codes));
            }
            // orgCode 和 branchCode 均为空时不加限制（admin 全量）

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
     * 获取账户详情
     */
    public AccountDetailResponse getAccountDetail(String loanAccount) {
        LoanAccount account = loanAccountRepository.findById(loanAccount)
                .orElseThrow(() -> new RuntimeException("未查询到账户信息"));

        AccountDetailResponse response = new AccountDetailResponse();
        response.setLoanAccount(account.getLoanAccount());
        response.setCustomerId(account.getCustomerId());
        response.setCustomerName(account.getCustomerName());
        response.setBranchName(account.getBranchName());
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

    /**
     * 统计未完成催收（uncollected + collecting）的客户数和贷款余额合计
     */
    public Map<String, Object> getStats(String branchCode, String orgCode) {
        List<Object[]> rows;
        if (branchCode != null && !branchCode.trim().isEmpty()) {
            rows = loanAccountRepository.statsActiveByBranchCode(branchCode.trim());
        } else if (orgCode != null && !orgCode.trim().isEmpty()) {
            List<String> codes = new java.util.ArrayList<>(
                    branchOrgRepository.findByOrgCode(orgCode.trim())
                            .stream().map(b -> b.getBranchCode()).collect(java.util.stream.Collectors.toList()));
            if (!codes.contains(orgCode.trim())) {
                codes.add(orgCode.trim());
            }
            rows = loanAccountRepository.statsActiveByBranchCodes(codes);
        } else {
            rows = loanAccountRepository.statsActiveAll();
        }
        Object[] row = (rows != null && !rows.isEmpty()) ? rows.get(0) : new Object[]{0L, null};
        long count = row[0] == null ? 0L : ((Number) row[0]).longValue();
        BigDecimal balance = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];

        // 按状态分组计数
        List<Object[]> statusCounts;
        if (branchCode != null && !branchCode.trim().isEmpty()) {
            statusCounts = loanAccountRepository.countByStatusForBranchCode(branchCode.trim());
        } else if (orgCode != null && !orgCode.trim().isEmpty()) {
            List<String> codes = new java.util.ArrayList<>(
                    branchOrgRepository.findByOrgCode(orgCode.trim())
                            .stream().map(b -> b.getBranchCode()).collect(java.util.stream.Collectors.toList()));
            if (!codes.contains(orgCode.trim())) {
                codes.add(orgCode.trim());
            }
            statusCounts = loanAccountRepository.countByStatusForBranchCodes(codes);
        } else {
            statusCounts = loanAccountRepository.countByStatusAll();
        }
        long uncollectedCount = 0, collectingCount = 0, completedCount = 0;
        for (Object[] sc : statusCounts) {
            String status = (String) sc[0];
            long c = ((Number) sc[1]).longValue();
            if ("uncollected".equals(status)) uncollectedCount = c;
            else if ("collecting".equals(status)) collectingCount = c;
            else if ("completed".equals(status)) completedCount = c;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("activeCount", count);
        result.put("totalLoanBalance", formatAmount(balance));
        result.put("uncollectedCount", uncollectedCount);
        result.put("collectingCount", collectingCount);
        result.put("completedCount", completedCount);
        return result;
    }

    private Map<String, Object> toListItem(LoanAccount account) {
        Map<String, Object> item = new HashMap<>();
        item.put("customerId", account.getCustomerId());
        item.put("loanAccount", account.getLoanAccount());
        item.put("customerName", account.getCustomerName());
        item.put("productCode", account.getProductCode());
        item.put("overdueDays", account.getOverdueDays());
        item.put("loanBalance", formatAmount(account.getLoanBalance()));
        item.put("status", account.getStatus());
        return item;
    }

    public void notifyNewOverdue(LoanAccount account, int overdueDays) {
        String title = "新增逾期通知";
        String message = String.format("贷款账号 %s 客户 %s 已进入逾期状态（宽限期结束），逾期天数 %d 天，请及时跟进。", account.getLoanAccount(), account.getCustomerName(), overdueDays);
        noticeService.createNotice(title, "high", message,
                account.getCustomerId(), account.getLoanAccount(), account.getCustomerName(), account.getProductCode(), overdueDays, "new_overdue",
                account.getBranchCode());
    }

    public void notifyCollectingCompleted(LoanAccount account) {
        String title = "逾期催收已完成还款";
        String message = String.format("贷款账号 %s 客户 %s 逾期 %d 天已完成还款，已转为已完成状态。", account.getLoanAccount(), account.getCustomerName(), account.getOverdueDays() == null ? 0 : account.getOverdueDays());
        noticeService.createNotice(title, "high", message,
                account.getCustomerId(), account.getLoanAccount(), account.getCustomerName(), account.getProductCode(), account.getOverdueDays(), "collecting_completed",
                account.getBranchCode());
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}
