package com.bank.lms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * 分支行实体（多对一 → 管辖行）
 * 同一分支行可挂在多个管辖行下，唯一约束为 (branch_code, org_code) 复合键
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "branch_org", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"branch_code", "org_code"})
})
public class BranchOrg extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_code", length = 20, nullable = false)
    private String branchCode;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "org_code", length = 20, nullable = false)
    private String orgCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_code", referencedColumnName = "org_code", insertable = false, updatable = false)
    private JurisdictionOrg jurisdiction;
}
