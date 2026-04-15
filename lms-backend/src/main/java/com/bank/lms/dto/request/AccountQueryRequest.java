package com.bank.lms.dto.request;

import lombok.Data;

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
    private String branchCode;
    // 管辖行机构号：不传 branchCode 时，后端自动查该管辖行下所有分支行做范围过滤
    private String orgCode;
    private PageRequest page;
}
