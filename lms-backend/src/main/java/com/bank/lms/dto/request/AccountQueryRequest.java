package com.bank.lms.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 账户查询请求
 */
@Data
public class AccountQueryRequest {
    private String customerId;
    private String loanAccount;
    private String productCode;
    private Integer overdueDays;
    private String status;
    private PageRequest page;
}
