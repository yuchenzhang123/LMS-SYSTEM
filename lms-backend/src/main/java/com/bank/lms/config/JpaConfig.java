package com.bank.lms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA配置
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.bank.lms.repository")
@EnableTransactionManagement
public class JpaConfig {
}
