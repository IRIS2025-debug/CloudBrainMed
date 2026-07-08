package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.service.OrderItemService;
import com.cloudbrainmed.doctor.vo.DoctorTaskDetailVo;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorTaskServiceImplTest {

    private final MedicalOrderMapper medicalOrderMapper =
            mock(MedicalOrderMapper.class);
    private final OrderItemService orderItemService =
            mock(OrderItemService.class);
    private final DoctorTaskServiceImpl service =
            new DoctorTaskServiceImpl(medicalOrderMapper, orderItemService);

    @Test
    void startTaskRejectsDoctorTypeMismatchBeforeClaiming() {
        MedicalOrderMapper.DoctorTaskDetailVo task = task("EXAM");
        task.setStatus("QUEUED");
        when(medicalOrderMapper.selectTaskDetailById("MOI_EXAM"))
                .thenReturn(task);

        assertThatThrownBy(() -> service.startTask("MOI_EXAM", "D003", 3))
                .hasMessageContaining("无权");
        verify(orderItemService, never()).claimTask("MOI_EXAM", "D003");
    }

    @Test
    void submitReportRejectsDoctorTypeMismatchBeforeSaving() {
        MedicalOrderMapper.DoctorTaskDetailVo task = task("LAB");
        task.setStatus("IN_PROCESS");
        task.setAssignedDoctorId("D002");
        when(medicalOrderMapper.selectTaskDetailById("MOI_LAB"))
                .thenReturn(task);
        MedicalReportSubmitRequest request = new MedicalReportSubmitRequest();
        request.setOrderItemId("MOI_LAB");
        request.setConclusion("Normal");

        assertThatThrownBy(() -> service.submitReport(request, "D002", 2))
                .hasMessageContaining("无权");
        verify(medicalOrderMapper, never()).insertMedicalReport(any());
    }

    @Test
    void completedTaskDetailCanBeViewedBySameDoctorTypeWhenAssignedToAnotherDoctor() {
        MedicalOrderMapper.DoctorTaskDetailVo task = task("EXAM");
        task.setStatus("COMPLETED");
        task.setAssignedDoctorId("D009");
        when(medicalOrderMapper.selectTaskDetailById("MOI_EXAM"))
                .thenReturn(task);
        MedicalReportVo report = new MedicalReportVo();
        report.setOrderItemId("MOI_EXAM");
        report.setConclusion("No acute abnormality");
        when(medicalOrderMapper.findPublishedReportByOrderItemId("MOI_EXAM"))
                .thenReturn(report);

        DoctorTaskDetailVo detail = service.getTaskDetail("MOI_EXAM", "D002", 2);

        assertThat(detail.getReport()).isSameAs(report);
    }

    @Test
    void startTaskAllowsMultipleInProgressTasksForSameDoctorAndPatient() {
        MedicalOrderMapper.DoctorTaskDetailVo task = task("EXAM");
        task.setStatus("QUEUED");
        when(medicalOrderMapper.selectTaskDetailById("MOI_EXAM"))
                .thenReturn(task);
        when(orderItemService.hasPatientInProgress("P001")).thenReturn(true);
        when(orderItemService.hasDoctorInProgress("D002")).thenReturn(true);
        when(orderItemService.claimTask("MOI_EXAM", "D002")).thenReturn(true);

        service.startTask("MOI_EXAM", "D002", 2);

        verify(orderItemService).claimTask("MOI_EXAM", "D002");
    }

    @Test
    void submitReportPublishesAndCompletesExamAndLabItems() {
        MedicalOrderMapper.DoctorTaskDetailVo exam = task("EXAM");
        exam.setStatus("IN_PROCESS");
        exam.setAssignedDoctorId("D002");
        MedicalOrderMapper.DoctorTaskDetailVo lab = task("LAB");
        lab.setStatus("IN_PROCESS");
        lab.setAssignedDoctorId("D003");
        when(medicalOrderMapper.selectTaskDetailById("MOI_EXAM"))
                .thenReturn(exam);
        when(medicalOrderMapper.selectTaskDetailById("MOI_LAB"))
                .thenReturn(lab);
        when(medicalOrderMapper.insertMedicalReport(any())).thenReturn(1);

        service.submitReport(reportRequest("MOI_EXAM"), "D002", 2);
        service.submitReport(reportRequest("MOI_LAB"), "D003", 3);

        verify(medicalOrderMapper, times(2)).insertMedicalReport(any());
        verify(medicalOrderMapper).completeTask("MOI_EXAM", "D002");
        verify(medicalOrderMapper).completeTask("MOI_LAB", "D003");
    }

    @Test
    void submitReportPersistsCtAiResultJson() {
        MedicalOrderMapper.DoctorTaskDetailVo exam = task("EXAM");
        exam.setStatus("IN_PROCESS");
        exam.setAssignedDoctorId("D002");
        when(medicalOrderMapper.selectTaskDetailById("MOI_EXAM"))
                .thenReturn(exam);
        when(medicalOrderMapper.insertMedicalReport(any())).thenReturn(1);
        MedicalReportSubmitRequest request = reportRequest("MOI_EXAM");
        request.setAiResultJson("{\"structured\":{\"artifact\":{\"ratio\":0.35}}}");

        MedicalReportVo result = service.submitReport(request, "D002", 2);

        org.mockito.ArgumentCaptor<com.cloudbrainmed.doctor.entity.MedicalReport> reportCaptor =
                org.mockito.ArgumentCaptor.forClass(com.cloudbrainmed.doctor.entity.MedicalReport.class);
        verify(medicalOrderMapper).insertMedicalReport(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getAiResultJson())
                .isEqualTo("{\"structured\":{\"artifact\":{\"ratio\":0.35}}}");
        assertThat(result.getAiResultJson())
                .isEqualTo("{\"structured\":{\"artifact\":{\"ratio\":0.35}}}");
    }

    private MedicalOrderMapper.DoctorTaskDetailVo task(String itemCategory) {
        MedicalOrderMapper.DoctorTaskDetailVo task =
                new MedicalOrderMapper.DoctorTaskDetailVo();
        task.setOrderItemId("MOI_" + itemCategory);
        task.setOrderId("MO001");
        task.setPatientId("P001");
        task.setRegisterId("REG001");
        task.setItemCategory(itemCategory);
        return task;
    }

    private MedicalReportSubmitRequest reportRequest(String orderItemId) {
        MedicalReportSubmitRequest request = new MedicalReportSubmitRequest();
        request.setOrderItemId(orderItemId);
        request.setResultSummary("Result summary");
        request.setConclusion("Conclusion");
        request.setAbnormalFlag("NORMAL");
        return request;
    }
}
