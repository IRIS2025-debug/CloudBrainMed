package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmRequest;
import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmResponse;
import com.cloudbrainmed.doctor.dto.MedicalOrderItemRequest;
import com.cloudbrainmed.doctor.dto.PrescriptionCreateDto;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.service.ConsultService;
import com.cloudbrainmed.doctor.service.MedicalOrderService;
import com.cloudbrainmed.doctor.service.PrescriptionService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultControllerTest {

    private final ConsultService consultService = mock(ConsultService.class);
    private final PrescriptionService prescriptionService = mock(PrescriptionService.class);
    private final MedicalOrderService medicalOrderService = mock(MedicalOrderService.class);
    private final ConsultController controller = new ConsultController(
            consultService, prescriptionService, medicalOrderService);

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

    @Test
    void confirmMedicalOrderUsesTokenDoctorAndReturnsQueueStatus() {
        MedicalOrderItemRequest item = new MedicalOrderItemRequest();
        item.setItemCode("CRANIAL_CT_PLAIN");
        MedicalOrderConfirmRequest request = new MedicalOrderConfirmRequest();
        request.setRegisterId("R001");
        request.setClinicalSummary("headache");
        request.setUrgencyLevel("NORMAL");
        request.setItems(List.of(item));
        MedicalOrderConfirmResponse response = new MedicalOrderConfirmResponse(
                "MO001", "MANUAL", 1, new BigDecimal("280.00"),
                "QUEUED", "PAID", true, null);
        when(medicalOrderService.confirm(request, "D001")).thenReturn(response);

        String token = DoctorJwtUtil.createToken("D001", "11111111111", 2, 1);

        Object result = controller.confirmMedicalOrder(token, request).getData();

        verify(medicalOrderService).confirm(request, "D001");
        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(response);
    }
}
