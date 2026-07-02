package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.dto.PrescriptionCreateDto;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.service.ConsultService;
import com.cloudbrainmed.doctor.service.PrescriptionService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultControllerTest {

    private final ConsultService consultService = mock(ConsultService.class);
    private final PrescriptionService prescriptionService = mock(PrescriptionService.class);
    private final ConsultController controller = new ConsultController(consultService, prescriptionService);

    @Test
    void createPrescriptionRejectsWhenDoctorDoesNotOwnRegistration() {
        PrescriptionCreateDto dto = new PrescriptionCreateDto();
        dto.setRegisterId("R001");
        String token = DoctorJwtUtil.createToken("D002", "11111111111", 2, 1);
        when(consultService.getDetail("D002", "R001")).thenThrow(new BusinessException("forbidden"));

        assertThrows(BusinessException.class, () -> controller.createPrescription(token, dto));

        verify(prescriptionService, never()).create(dto, "D002", "D002", null, null);
    }

    @Test
    void createPrescriptionRejectsCompletedConsult() {
        PrescriptionCreateDto dto = new PrescriptionCreateDto();
        dto.setRegisterId("R001");
        String token = DoctorJwtUtil.createToken("D001", "11111111111", 2, 1);
        ConsultRecord record = new ConsultRecord();
        record.setConsultStatus("COMPLETED");
        when(consultService.getDetail("D001", "R001")).thenReturn(record);

        assertThrows(BusinessException.class, () -> controller.createPrescription(token, dto));

        verify(prescriptionService, never()).create(dto, "D001", "D001", null, null);
    }

    @Test
    void createPrescriptionFillsPatientAndDoctorDisplayFieldsFromConsult() {
        PrescriptionCreateDto dto = new PrescriptionCreateDto();
        dto.setRegisterId("R001");
        String token = DoctorJwtUtil.createToken("D001", "11111111111", 2, 1);
        ConsultRecord record = new ConsultRecord();
        record.setConsultStatus("IN_PROGRESS");
        record.setPatientId("P001");
        record.setName("patient");
        record.setDoctorName("doctor");
        when(consultService.getDetail("D001", "R001")).thenReturn(record);

        controller.createPrescription(token, dto);

        verify(prescriptionService).create(dto, "D001", "doctor", "P001", "patient");
    }

    @Test
    void prescriptionListRejectsWhenDoctorDoesNotOwnRegistration() {
        when(consultService.getDetail("D002", "R001")).thenThrow(new BusinessException("forbidden"));

        assertThrows(BusinessException.class, () -> controller.prescriptionList("D002", "R001"));

        verify(prescriptionService, never()).getByRegisterId("R001");
    }

    @Test
    void createExamOrderUsesTokenDoctorForOwnershipCheck() {
        Map<String, String> body = Map.of(
                "registerId", "R001",
                "checkItemList", "[{\"itemName\":\"CT\"}]",
                "urgencyLevel", "NORMAL");

        String token = DoctorJwtUtil.createToken("D001", "11111111111", 2, 1);

        controller.createExamOrder(token, body);

        verify(consultService).createExamOrder("D001", "R001", "[{\"itemName\":\"CT\"}]", "NORMAL");
    }
}
