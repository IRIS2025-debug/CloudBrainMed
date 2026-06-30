package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.exception.BusinessException;
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
        when(consultService.getDetail("D002", "R001")).thenThrow(new BusinessException("forbidden"));

        assertThrows(BusinessException.class, () -> controller.createPrescription("D002", dto));

        verify(prescriptionService, never()).create(dto, "D002", "D002");
    }

    @Test
    void createPrescriptionRejectsCompletedConsult() {
        PrescriptionCreateDto dto = new PrescriptionCreateDto();
        dto.setRegisterId("R001");
        ConsultRecord record = new ConsultRecord();
        record.setConsultStatus("COMPLETED");
        when(consultService.getDetail("D001", "R001")).thenReturn(record);

        assertThrows(BusinessException.class, () -> controller.createPrescription("D001", dto));

        verify(prescriptionService, never()).create(dto, "D001", "D001");
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

        controller.createExamOrder("D001", body);

        verify(consultService).createExamOrder("D001", "R001", "[{\"itemName\":\"CT\"}]", "NORMAL");
    }
}
