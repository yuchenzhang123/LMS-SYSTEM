package com.bank.lms.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * 管辖行实体
 */
@Data
@Entity
@Table(name = "jurisdiction_org")
public class JurisdictionOrg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_code", length = 20, nullable = false, unique = true)
    private String orgCode;

    @Column(name = "org_name", length = 100)
    private String orgName;
}
