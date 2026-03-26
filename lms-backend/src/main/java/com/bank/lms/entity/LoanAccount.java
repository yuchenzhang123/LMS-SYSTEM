package com.bank.lms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 贷款账户实体（存储在GaussDB，从GBase转换后存储）
 */
@Data
@Entity
@Table(name = "loan_account")
@EqualsAndHashCode(callSuper = true)
public class LoanAccount extends BaseEntity {

    @Id
    @Column(name = "loan_account", length = 32)
    private String loanAccount;

    @Column(name = "customer_id", length = 32, nullable = false)
    private String customerId;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "org_name", length = 200)
    private String orgName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "product_code", length = 32)
    private String productCode;

    @Column(name = "product_name", length = 100)
    private String productName;

    @Column(name = "loan_date")
    private LocalDate loanDate;

    @Column(name = "loan_term")
    private Integer loanTerm;

    @Column(name = "overdue_days")
    private Integer overdueDays = 0;

    @Column(name = "overdue_times")
    private Integer overdueTimes = 0;

    @Column(name = "contract_amount", precision = 18, scale = 2)
    private BigDecimal contractAmount;

    @Column(name = "loan_balance", precision = 18, scale = 2)
    private BigDecimal loanBalance;

    @Column(name = "unexpired_principal", precision = 18, scale = 2)
    private BigDecimal unexpiredPrincipal;

    @Column(name = "overdue_principal", precision = 18, scale = 2)
    private BigDecimal overduePrincipal;

    @Column(name = "overdue_interest", precision = 18, scale = 2)
    private BigDecimal overdueInterest;

    @Column(name = "overdue_penalty", precision = 18, scale = 2)
    private BigDecimal overduePenalty;

    @Column(name = "total_overdue_amount", precision = 18, scale = 2)
    private BigDecimal totalOverdueAmount;

    @Column(name = "status", length = 20)
    private String status = "uncollected";

    @Column(name = "expected_days")
    private Integer expectedDays = 0;

    @Column(name = "status_update_time")
    private LocalDateTime statusUpdateTime;

    @Column(name = "gbase_sync_time")
    private LocalDateTime gbaseSyncTime;

    @Column(name = "gbase_raw_data", columnDefinition = "TEXT")
    private String gbaseRawData;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;
}
