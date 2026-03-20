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

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}
