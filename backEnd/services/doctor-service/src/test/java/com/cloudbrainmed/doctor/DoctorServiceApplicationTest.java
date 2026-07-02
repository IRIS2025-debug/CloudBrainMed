package com.cloudbrainmed.doctor;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.EnableFeignClients;

import static org.assertj.core.api.Assertions.assertThat;

class DoctorServiceApplicationTest {

    @Test
    void enablesServiceApiFeignClients() {
        EnableFeignClients annotation =
                DoctorServiceApplication.class.getAnnotation(EnableFeignClients.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.basePackages())
                .contains("com.cloudbrainmed.api.feign");
    }
}
