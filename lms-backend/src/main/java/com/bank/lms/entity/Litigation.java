package com.bank.lms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * 诉讼信息实体
 */
@Data
@Entity
@Table(name = "litigation")
@EqualsAndHashCode(callSuper = true)
public class Litigation extends BaseEntity {

    @Id
    @Column(name = "litigation_id", length = 32)
    private String litigationId;

    @Column(name = "loan_account", length = 32, nullable = false)
    private String loanAccount;

    @Column(name = "customer_id", length = 32, nullable = false)
    private String customerId;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "status_code", length = 20, nullable = false)
    private String statusCode;

    @Column(name = "status_text", length = 200)
    private String statusText;

    @Column(name = "in_litigation")
    private Boolean inLitigation = false;

    // 律所提交阶段
    @Column(name = "submit_to_law_firm_date", length = 20)
    private String submitToLawFirmDate;

    @Column(name = "submit_to_court_date", length = 20)
    private String submitToCourtDate;

    @Column(name = "filing_case_no", length = 100)
    private String filingCaseNo;

    // 开庭判决阶段
    @Column(name = "is_hearing")
    private Boolean isHearing = false;

    @Column(name = "hearing_date", length = 20)
    private String hearingDate;

    @Column(name = "judgment_date", length = 20)
    private String judgmentDate;

    // 执行阶段
    @Column(name = "execution_apply_to_court_date", length = 20)
    private String executionApplyToCourtDate;

    @Column(name = "execution_filing_date", length = 20)
    private String executionFilingDate;

    @Column(name = "execution_case_no", length = 100)
    private String executionCaseNo;

    @Column(name = "auction_status", length = 50)
    private String auctionStatus;

    // 费用信息
    @Column(name = "litigation_fee", precision = 18, scale = 2)
    private BigDecimal litigationFee = BigDecimal.ZERO;

    @Column(name = "litigation_fee_paid_by_customer")
    private Boolean litigationFeePaidByCustomer = false;

    @Column(name = "preservation_fee", precision = 18, scale = 2)
    private BigDecimal preservationFee = BigDecimal.ZERO;

    @Column(name = "preservation_fee_paid_by_customer")
    private Boolean preservationFeePaidByCustomer = false;

    @Column(name = "appraisal_fee", precision = 18, scale = 2)
    private BigDecimal appraisalFee = BigDecimal.ZERO;

    @Column(name = "litigation_preservation_paid_at", length = 20)
    private String litigationPreservationPaidAt;

    @Column(name = "litigation_preservation_write_off_at", length = 20)
    private String litigationPreservationWriteOffAt;

    @Column(name = "lawyer_fee", precision = 18, scale = 2)
    private BigDecimal lawyerFee = BigDecimal.ZERO;

    @Column(name = "lawyer_fee_paid_by_customer")
    private Boolean lawyerFeePaidByCustomer = false;

    // 机构信息
    @Column(name = "court_name", length = 200)
    private String courtName;

    @Column(name = "law_firm", length = 200)
    private String lawFirm;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;
}
