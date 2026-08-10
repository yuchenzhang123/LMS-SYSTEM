package com.bank.lms.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 员工登录记录
 */
@Data
@Entity
@Table(name = "user_login_log")
public class UserLoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ehr_no", length = 50, nullable = false)
    private String ehrNo;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "org_code", length = 20)
    private String orgCode;

    @Column(name = "login_time")
    private LocalDateTime loginTime;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @PrePersist
    protected void onCreate() {
        if (loginTime == null) {
            loginTime = LocalDateTime.now();
        }
    }
}
