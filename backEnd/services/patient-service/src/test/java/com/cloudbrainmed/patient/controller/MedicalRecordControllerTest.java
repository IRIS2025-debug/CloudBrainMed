package com.cloudbrainmed.patient.controller;

import com.cloudbrainmed.common.utils.JwtUtil;
import com.cloudbrainmed.patient.service.MedicalRecordService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicalRecordControllerTest {

    @Test
    void myListUsesPatientIdFromTokenInsteadOfQueryParam() {
        MedicalRecordService medicalRecordService = mock(MedicalRecordService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        MedicalRecordController controller = new MedicalRecordController(
                medicalRecordService, jwtUtil);
        when(jwtUtil.getPatientIdFromToken("token-p001")).thenReturn("P001");
        when(medicalRecordService.getByPatientId("P001")).thenReturn(List.of());

        controller.listByPatientId("token-p001", "P004");

        verify(medicalRecordService).getByPatientId("P001");
    }

    @Test
    void myListRejectsMissingToken() {
        MedicalRecordController controller = new MedicalRecordController(
                mock(MedicalRecordService.class), mock(JwtUtil.class));

        assertThatThrownBy(() -> controller.listByPatientId(null, "P004"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("未登录，请先登录");
    }
    @Test
    void listByRegisterIdUsesPatientIdFromToken() {
        MedicalRecordService medicalRecordService = mock(MedicalRecordService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        MedicalRecordController controller = new MedicalRecordController(
                medicalRecordService, jwtUtil);
        when(jwtUtil.getPatientIdFromToken("token-p001")).thenReturn("P001");
        when(medicalRecordService.getByRegisterId("R001", "P001")).thenReturn(List.of());

        controller.listByRegisterId("token-p001", "R001");

        verify(medicalRecordService).getByRegisterId("R001", "P001");
    }
}
