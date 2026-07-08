package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmRequest;
import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmResponse;
import com.cloudbrainmed.doctor.dto.PrescriptionCreateDto;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.entity.Prescription;
import com.cloudbrainmed.doctor.service.ConsultService;
import com.cloudbrainmed.doctor.service.MedicalOrderService;
import com.cloudbrainmed.doctor.service.PrescriptionService;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultControllerReportTest {

    private final ConsultService consultService = mock(ConsultService.class);
    private final PrescriptionService prescriptionService = mock(PrescriptionService.class);
    private final MedicalOrderService medicalOrderService = mock(MedicalOrderService.class);
    private final ConsultController controller = new ConsultController(
            consultService, prescriptionService, medicalOrderService);

    @Test
    void receptionDoctorReadsReturnedExamAndLabReportsByRegisterId() {
        String token = DoctorJwtUtil.createToken("D001", "11111111111", 2, 1);
        ConsultRecord detail = new ConsultRecord();
        detail.setRegisterId("REG001");
        detail.setDoctorId("D001");
        when(consultService.getDetail("D001", "REG001")).thenReturn(detail);
        when(medicalOrderService.getPublishedReportsByRegisterId("REG001"))
                .thenReturn(List.of(report("MOI_CT", "EXAM"), report("MOI_LAB", "LAB")));

        Result<?> result = assertDoesNotThrow(() -> controller.reports(token, "REG001"));

        verify(consultService).getDetail("D001", "REG001");
        verify(medicalOrderService).getPublishedReportsByRegisterId("REG001");
        assertThat((List<?>) result.getData()).hasSize(2);
        assertThat((List<MedicalReportVo>) result.getData())
                .extracting(MedicalReportVo::getItemCategory)
                .containsExactly("EXAM", "LAB");
    }

    private MedicalReportVo report(String orderItemId, String itemCategory) {
        MedicalReportVo report = new MedicalReportVo();
        report.setOrderItemId(orderItemId);
        report.setRegisterId("REG001");
        report.setItemCategory(itemCategory);
        report.setStatus("PUBLISHED");
        return report;
    }
}
