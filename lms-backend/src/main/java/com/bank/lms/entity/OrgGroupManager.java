package com.bank.lms.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * 范围组管理人员实体（人员维度）
 * 组内管理人员可绕过自身机构限制，查看全组数据
 */
@Data
@Entity
@Table(name = "org_group_manager", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_code", "ehr_no"})
})
public class OrgGroupManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_code", length = 50, nullable = false)
    private String groupCode;

    @Column(name = "ehr_no", length = 50, nullable = false)
    private String ehrNo;

    @Column(name = "user_name", length = 100)
    private String userName;
}
