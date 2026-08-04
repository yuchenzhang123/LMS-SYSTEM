package com.bank.lms.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * 范围组实体
 * 同一真实机构下的多个机构号归入同一个范围组，组内互相可见
 */
@Data
@Entity
@Table(name = "org_group")
public class OrgGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_code", length = 50, nullable = false, unique = true)
    private String groupCode;

    @Column(name = "group_name", length = 100, nullable = false)
    private String groupName;
}
