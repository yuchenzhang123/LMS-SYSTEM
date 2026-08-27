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
    // 人员 EHR 号：用于后端按角色解析数据范围（组管理员判断需要）
    private String ehrNo;
    /** 排序方式：time(按创建时间)/priority(智能排序)/amount(按总逾期金额) */
    private String sortBy;
    private PageRequest page;
}
