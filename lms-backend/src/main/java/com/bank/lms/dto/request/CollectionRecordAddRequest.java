package com.bank.lms.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 催收记录新增请求
 */
@Data
public class CollectionRecordAddRequest {
    @NotBlank(message = "贷款账户不能为空")
    private String loanAccount;

    @NotBlank(message = "客户ID不能为空")
    private String customerId;

    private String targetType;

    private String targetName;

    private String actualCollectionTime;

    @NotBlank(message = "催收方式不能为空")
    private String method;

    @NotBlank(message = "催收方式文本不能为空")
    private String methodText;

    @NotBlank(message = "催收结果不能为空")
    private String result;

    private String operatorId;
    private String operatorName;
    private String time;
    private String remark;
    private String materialType;
    private String materialName;
    private String materialUrl;
}
