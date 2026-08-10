package com.bank.lms.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户-机构映射（每日从 GBase R_V_O_USER_BASIC 同步）
 */
@Data
@Entity
@Table(name = "user_org")
public class UserOrg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ehr_no", length = 50, nullable = false, unique = true)
    private String ehrNo;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "org_code", length = 20, nullable = false)
    private String orgCode;

    @Column(name = "org_name", length = 100)
    private String orgName;

    @Column(name = "status", length = 20)
    private String status = "active";

    @Column(name = "gbase_sync_time")
    private LocalDateTime gbaseSyncTime;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
