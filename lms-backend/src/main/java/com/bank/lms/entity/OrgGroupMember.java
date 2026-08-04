package com.bank.lms.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * 范围组成员实体（机构号维度）
 * 组内成员平级，isManagerOrg 标记是否为管辖机构
 */
@Data
@Entity
@Table(name = "org_group_member", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_code", "org_code"})
})
public class OrgGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_code", length = 50, nullable = false)
    private String groupCode;

    @Column(name = "org_code", length = 20, nullable = false)
    private String orgCode;

    @Column(name = "org_name", length = 100)
    private String orgName;

    @Column(name = "is_manager_org")
    private Boolean isManagerOrg = false;
}
