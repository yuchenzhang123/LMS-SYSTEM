package com.bank.lms.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * 多数据源配置：外部GBase（JdbcTemplate）、内部GaussDB/MySQL（JPA）
 */
@Configuration
@EnableTransactionManagement
@Slf4j
public class MultiDataSourceConfig {

    // ========== 外部数据源：GBase ==========
    @Bean(name = "gbaseDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.gbase")
    public DataSource gbaseDataSource() {
        return new DruidDataSource();
    }

    // GBase的JdbcTemplate（用于直接查询返回Map）
    @Bean(name = "gbaseJdbcTemplate")
    public JdbcTemplate gbaseJdbcTemplate(@Qualifier("gbaseDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ========== 内部数据源：GaussDB/MySQL（主数据源，@Primary标记） ==========
    @Primary
    @Bean(name = "mainDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.main")
    public DruidDataSource mainDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        log.info("主数据源配置加载中，将在初始化时显示具体URL");
        return dataSource;
    }

    // GaussDB/MySQL的EntityManagerFactory（JPA核心）
    @Primary
    @Bean(name = "mainEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean mainEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("mainDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.bank.lms.entity")
                .persistenceUnit("mainPersistenceUnit")
                .build();
    }

    // GaussDB/MySQL的事务管理器（JPA事务）
    @Primary
    @Bean(name = "mainTransactionManager")
    public PlatformTransactionManager mainTransactionManager(
            @Qualifier("mainEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    // 启用JPA仓库（指定Repository包路径）
    @EnableJpaRepositories(
            basePackages = "com.bank.lms.repository",
            entityManagerFactoryRef = "mainEntityManagerFactory",
            transactionManagerRef = "mainTransactionManager"
    )
    static class JpaRepositoryConfig {}
}
