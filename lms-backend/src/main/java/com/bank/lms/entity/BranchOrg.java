package com.bank.lms.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * 分支行实体（多对一 → 管辖行）
 */
@Data
@Entity
@Table(name = "branch_org")
public class BranchOrg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_code", length = 20, nullable = false, unique = true)
    private String branchCode;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "org_code", length = 20, nullable = false)
    private String orgCode;
}
