package com.cloudbrainmed.payment.service.impl;

import com.cloudbrainmed.payment.feign.DoctorServiceFeignClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MedicalOrderCallbackServiceImplTest {

    @Test
    void returnsTrueWhenDoctorServiceResultDataIsTrue() {
        DoctorServiceFeignClient feignClient = mock(DoctorServiceFeignClient.class);
        MedicalOrderCallbackServiceImpl service =
                new MedicalOrderCallbackServiceImpl(feignClient);
        when(feignClient.onPaymentSuccess("MO001"))
                .thenReturn(Map.of("code", 200, "msg", "success", "data", true));

        assertThat(service.onOrderPaid("MO001")).isTrue();
    }
}
