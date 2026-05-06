package com.bank.lms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置类
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
    // 启用 Spring 定时任务 + 异步执行功能
}
