package com.cloudbrainmed.patient.controller;

import com.cloudbrainmed.common.utils.JwtUtil;
import com.cloudbrainmed.patient.service.PrescriptionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrescriptionControllerTest {

    @Test
    void myListUsesPatientIdFromTokenInsteadOfQueryParam() {
        PrescriptionService prescriptionService = mock(PrescriptionService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        PrescriptionController controller = new PrescriptionController(
                prescriptionService, jwtUtil);
        when(jwtUtil.getPatientIdFromToken("token-p001")).thenReturn("P001");
        when(prescriptionService.getByPatientId("P001")).thenReturn(List.of());

        controller.listByPatientId("token-p001", "P004");

        verify(prescriptionService).getByPatientId("P001");
    }

    @Test
    void myListRejectsMissingToken() {
        PrescriptionController controller = new PrescriptionController(
                mock(PrescriptionService.class), mock(JwtUtil.class));

        assertThatThrownBy(() -> controller.listByPatientId(null, "P004"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("未登录，请先登录");
    }
    @Test
    void listByRegisterIdUsesPatientIdFromToken() {
        PrescriptionService prescriptionService = mock(PrescriptionService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        PrescriptionController controller = new PrescriptionController(
                prescriptionService, jwtUtil);
        when(jwtUtil.getPatientIdFromToken("token-p001")).thenReturn("P001");
        when(prescriptionService.getByRegisterId("R001", "P001")).thenReturn(List.of());

        controller.listByRegisterId("token-p001", "R001");

        verify(prescriptionService).getByRegisterId("R001", "P001");
    }
}
