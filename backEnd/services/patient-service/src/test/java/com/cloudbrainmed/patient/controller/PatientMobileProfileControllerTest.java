package com.cloudbrainmed.patient.controller;

import com.cloudbrainmed.api.feign.PaymentFeignClient;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.JwtUtil;
import com.cloudbrainmed.patient.entity.Registration;
import com.cloudbrainmed.patient.module5.service.PatientProfileService;
import com.cloudbrainmed.patient.service.RegisterService;
import com.cloudbrainmed.payment.vo.PayResultVo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatientMobileProfileControllerTest {

    @Test
    void paymentsReturnsPaymentResultDataWithoutDoubleWrapping() {
        PatientProfileService profileService = mock(PatientProfileService.class);
        RegisterService registerService = mock(RegisterService.class);
        PaymentFeignClient paymentFeignClient = mock(PaymentFeignClient.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        PatientMobileProfileController controller = new PatientMobileProfileController(
                profileService, registerService, paymentFeignClient, jwtUtil);
        List<PayResultVo> payments = List.of(new PayResultVo());
        when(jwtUtil.getPatientIdFromToken("token")).thenReturn("P001");
        when(paymentFeignClient.getPayHistory("P001")).thenReturn(Result.ok(payments));

        Result<?> result = controller.payments("token");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isSameAs(payments);
    }

    @Test
    void registersReturnsRegisterHistoryForTokenPatientId() {
        PatientProfileService profileService = mock(PatientProfileService.class);
        RegisterService registerService = mock(RegisterService.class);
        PaymentFeignClient paymentFeignClient = mock(PaymentFeignClient.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        PatientMobileProfileController controller = new PatientMobileProfileController(
                profileService, registerService, paymentFeignClient, jwtUtil);
        List<Registration> history = List.of(new Registration());
        when(jwtUtil.getPatientIdFromToken("token")).thenReturn("P001");
        when(registerService.getRegisterHistory("P001")).thenReturn(history);

        Result<?> result = controller.registers("token");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isSameAs(history);
    }

    @Test
    void profileEndpointsRejectRawPatientIdToken() {
        PatientProfileService profileService = mock(PatientProfileService.class);
        RegisterService registerService = mock(RegisterService.class);
        PaymentFeignClient paymentFeignClient = mock(PaymentFeignClient.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        PatientMobileProfileController controller = new PatientMobileProfileController(
                profileService, registerService, paymentFeignClient, jwtUtil);
        when(jwtUtil.getPatientIdFromToken("P001")).thenThrow(new RuntimeException("invalid token"));

        assertThatThrownBy(() -> controller.info("P001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Token 无效，请重新登录");
    }
}
