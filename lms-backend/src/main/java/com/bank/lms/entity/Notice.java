package com.bank.lms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * 系统通知实体
 */
@Data
@Entity
@Table(name = "notice")
@EqualsAndHashCode(callSuper = true)
public class Notice extends BaseEntity {

    @Id
    @Column(name = "notice_id", length = 32)
    private String noticeId;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "customer_id", length = 32)
    private String customerId;

    @Column(name = "loan_account", length = 32)
    private String loanAccount;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "product_code", length = 32)
    private String productCode;

    @Column(name = "overdue_days")
    private Integer overdueDays;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;
}
