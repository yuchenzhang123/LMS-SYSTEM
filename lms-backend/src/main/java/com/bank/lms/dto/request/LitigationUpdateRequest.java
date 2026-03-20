package com.bank.lms.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 诉讼信息更新请求
 */
@Data
public class LitigationUpdateRequest {
    private String litigationId;

    @NotBlank(message = "贷款账户不能为空")
    private String loanAccount;

    @NotBlank(message = "客户ID不能为空")
    private String customerId;

    @NotBlank(message = "诉讼状态编码不能为空")
    private String statusCode;

    @NotBlank(message = "诉讼状态名称不能为空")
    private String statusText;

    private Boolean inLitigation;
    private String submitToLawFirmDate;
    private String submitToCourtDate;
    private String filingCaseNo;
    private Boolean isHearing;
    private String hearingDate;
    private String judgmentDate;
    private String executionApplyToCourtDate;
    private String executionFilingDate;
    private String executionCaseNo;
    private String auctionStatus;
    private String litigationFee;
    private Boolean litigationFeePaidByCustomer;
    private String preservationFee;
    private Boolean preservationFeePaidByCustomer;
    private String appraisalFee;
    private String litigationPreservationPaidAt;
    private String litigationPreservationWriteOffAt;
    private String lawyerFee;
    private Boolean lawyerFeePaidByCustomer;
    private String courtName;
    private String lawFirm;
    private String remark;
    private String operatorId;
    private String operatorName;
}
