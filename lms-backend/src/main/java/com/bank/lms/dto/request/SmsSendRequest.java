package com.bank.lms.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 短信发送请求
 */
@Data
public class SmsSendRequest {
    @NotBlank(message = "贷款账户不能为空")
    private String loanAccount;

    @NotBlank(message = "客户ID不能为空")
    private String customerId;

    private String templateId;

    @NotBlank(message = "短信内容不能为空")
    private String content;

    private String phone;

    /** 操作员ID */
    private String operatorId;

    /** 操作员姓名 */
    private String operatorName;
}
