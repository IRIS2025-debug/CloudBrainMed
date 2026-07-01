package com.cloudbrainmed.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@EnableFeignClients
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.cloudbrainmed.auth",
        "com.cloudbrainmed.common"
}, excludeFilters = @ComponentScan.Filter(
        // auth 自带 GlobalExceptionHandler，排除 common 中同名的，避免 bean 名冲突
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.cloudbrainmed.common.exception.GlobalExceptionHandler.class))
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
