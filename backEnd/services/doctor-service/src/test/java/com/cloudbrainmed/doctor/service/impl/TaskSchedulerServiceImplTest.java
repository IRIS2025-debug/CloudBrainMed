package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.service.DoctorTaskService;
import com.cloudbrainmed.doctor.service.QueueService;
import com.cloudbrainmed.doctor.service.SchedulerService;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskSchedulerServiceImplTest {

    private TaskSchedulerServiceImpl createService(
            MedicalOrderMapper medicalOrderMapper,
            QueueService queueService) {
        return createService(
                medicalOrderMapper, queueService,
                mock(DoctorTaskService.class));
    }

    private TaskSchedulerServiceImpl createService(
            MedicalOrderMapper medicalOrderMapper,
            QueueService queueService,
            DoctorTaskService doctorTaskService) {
        return new TaskSchedulerServiceImpl(
                doctorTaskService,
                queueService,
                mock(SchedulerService.class),
                medicalOrderMapper);
    }

    @Test
    void enqueueByPaymentMarksOrderPaidAndQueuesMainOrderAndItems() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        QueueService queueService = mock(QueueService.class);
        TaskSchedulerServiceImpl service = createService(
                medicalOrderMapper, queueService);
        when(medicalOrderMapper.updatePayStatus("MO001")).thenReturn(1);
        when(medicalOrderMapper.enqueueOrder("MO001")).thenReturn(1);
        when(medicalOrderMapper.enqueueOrderItems("MO001")).thenReturn(2);

        service.enqueueByPayment("MO001");

        verify(medicalOrderMapper).updatePayStatus("MO001");
        verify(medicalOrderMapper).enqueueOrder("MO001");
        verify(medicalOrderMapper).enqueueOrderItems("MO001");
    }

    @Test
    void enqueueByPaymentStillQueuesWhenOrderWasAlreadyMarkedPaid() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        QueueService queueService = mock(QueueService.class);
        TaskSchedulerServiceImpl service = createService(
                medicalOrderMapper, queueService);
        when(medicalOrderMapper.updatePayStatus("MO001")).thenReturn(0);
        when(medicalOrderMapper.enqueueOrder("MO001")).thenReturn(1);
        when(medicalOrderMapper.enqueueOrderItems("MO001")).thenReturn(2);

        service.enqueueByPayment("MO001");

        verify(medicalOrderMapper).updatePayStatus("MO001");
        verify(medicalOrderMapper).enqueueOrder("MO001");
        verify(medicalOrderMapper).enqueueOrderItems("MO001");
    }

    @Test
    void submitReportDelegatesToDoctorTaskService() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        QueueService queueService = mock(QueueService.class);
        DoctorTaskService doctorTaskService = mock(DoctorTaskService.class);
        TaskSchedulerServiceImpl service = createService(
                medicalOrderMapper, queueService, doctorTaskService);

        MedicalReportSubmitRequest request = new MedicalReportSubmitRequest();
        request.setOrderItemId("MOI001");
        request.setResultSummary("No acute intracranial abnormality.");
        request.setConclusion("Normal cranial CT.");
        request.setAbnormalFlag("NORMAL");
        MedicalReportVo expected = new MedicalReportVo();
        expected.setOrderItemId("MOI001");
        expected.setStatus("PUBLISHED");
        when(doctorTaskService.submitReport(request, "D002", 2))
                .thenReturn(expected);

        MedicalReportVo report = service.submitReport(request, "D002", 2);

        verify(doctorTaskService).submitReport(request, "D002", 2);
        assertThat(report.getOrderItemId()).isEqualTo("MOI001");
        assertThat(report.getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void completeTaskDelegatesToDoctorTaskService() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        QueueService queueService = mock(QueueService.class);
        DoctorTaskService doctorTaskService = mock(DoctorTaskService.class);
        TaskSchedulerServiceImpl service = createService(
                medicalOrderMapper, queueService, doctorTaskService);

        service.completeTask("MOI001", "D002", 2);

        verify(doctorTaskService).completeTask("MOI001", "D002", 2);
    }
}
