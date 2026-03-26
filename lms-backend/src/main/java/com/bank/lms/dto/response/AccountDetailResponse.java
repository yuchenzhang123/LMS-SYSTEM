package com.bank.lms.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户详情响应
 */
@Data
public class AccountDetailResponse {
    private String loanAccount;
    private String customerId;
    private String customerName;
    private String orgName;
    private String phone;
    private String productCode;
    private String productName;
    private String loanDate;
    private Integer loanTerm;
    private Integer overdueDays;
    private Integer overdueTimes;
    private String contractAmount;
    private String loanBalance;
    private String unexpiredPrincipal;
    private String overduePrincipal;
    private String overdueInterest;
    private String overduePenalty;
    private String totalOverdueAmount;
    private String status;
}
