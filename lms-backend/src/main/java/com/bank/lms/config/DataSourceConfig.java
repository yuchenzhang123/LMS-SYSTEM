package com.bank.lms.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 多数据源配置
 * 主数据源：GaussDB/MySQL（LMS业务数据库）
 * GBase数据源：用于同步外部数据
 */
@Slf4j
@Configuration
public class DataSourceConfig {

    /**
     * 主数据源（GaussDB/MySQL）
     */
    @Primary
    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        log.info("初始化主数据源");
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * GBase数据源（用于同步外部数据）
     * 仅在配置了spring.datasource.gbase.url时启用
     */
    @Bean(name = "gbaseDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.gbase")
    @ConditionalOnProperty(prefix = "spring.datasource.gbase", name = "url")
    public DataSource gbaseDataSource() {
        log.info("初始化GBase数据源");
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * 主数据源JdbcTemplate
     */
    @Primary
    @Bean(name = "jdbcTemplate")
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * GBase数据源JdbcTemplate
     * 用于GbaseSyncService同步外部数据
     */
    @Bean(name = "gbaseJdbcTemplate")
    @ConditionalOnProperty(prefix = "spring.datasource.gbase", name = "url")
    public JdbcTemplate gbaseJdbcTemplate(@Qualifier("gbaseDataSource") DataSource gbaseDataSource) {
        return new JdbcTemplate(gbaseDataSource);
    }

    /**
     * 主数据源事务管理器
     */
    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
