package com.bank.lms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 管辖行实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "jurisdiction_org")
public class JurisdictionOrg extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_code", length = 20, nullable = false, unique = true)
    private String orgCode;

    @Column(name = "org_name", length = 100)
    private String orgName;

    @OneToMany(mappedBy = "jurisdiction", fetch = FetchType.LAZY)
    private List<BranchOrg> branches = new ArrayList<>();
}
