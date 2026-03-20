package com.bank.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableScheduling
public class LmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(LmsApplication.class, args);
    }
}