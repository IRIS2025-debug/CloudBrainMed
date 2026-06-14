package com.cloudbrainmed.patient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableFeignClients(basePackages = "com.cloudbrainmed.api.feign")
@SpringBootApplication
@ComponentScan(basePackages = {"com.cloudbrainmed.patient", "com.cloudbrainmed.common"})
public class PatientServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PatientServiceApplication.class, args);
    }
}
