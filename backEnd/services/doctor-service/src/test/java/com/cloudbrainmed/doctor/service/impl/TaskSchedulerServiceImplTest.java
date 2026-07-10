package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.entity.DoctorSkill;
import com.cloudbrainmed.doctor.mapper.DoctorSkillMapper;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.entity.MedicalReport;
import com.cloudbrainmed.doctor.service.AgingService;
import com.cloudbrainmed.doctor.service.DoctorTaskService;
import com.cloudbrainmed.doctor.service.OrderItemService;
import com.cloudbrainmed.doctor.service.QueueService;
import com.cloudbrainmed.doctor.service.SchedulerService;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;
import com.cloudbrainmed.doctor.vo.DoctorTaskVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskSchedulerServiceImplTest {

    private TaskSchedulerServiceImpl createService(
            MedicalOrderMapper medicalOrderMapper,
            QueueService queueService) {
        return createService(
                medicalOrderMapper,
                queueService,
                mock(DoctorTaskService.class));
    }

    private TaskSchedulerServiceImpl createService(
            MedicalOrderMapper medicalOrderMapper,
            QueueService queueService,
            DoctorTaskService doctorTaskService) {
        return new TaskSchedulerServiceImpl(
                doctorTaskService,
                queueService,
                mock(OrderItemService.class),
                mock(SchedulerService.class),
                mock(AgingService.class),
                medicalOrderMapper);
    }

    @Test
    void getAssignableQueueReturnsExamTasksForExamDoctor() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        DoctorSkillMapper doctorSkillMapper = mock(DoctorSkillMapper.class);
        QueueService queueService = mock(QueueService.class);
        TaskSchedulerServiceImpl service = createService(medicalOrderMapper, queueService);
        MedicalOrderMapper.QueuedTaskItem item = queuedItem("ITEM001", "EXAM");
        when(queueService.getAssignableQueue(eq("D001"), eq(2)))
                .thenReturn(List.of(convertToVo(item)));

        List<DoctorTaskVo> tasks = service.getAssignableQueue("D001", 2);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getOrderItemId()).isEqualTo("ITEM001");
        assertThat(tasks.get(0).getItemCategory()).isEqualTo("EXAM");
        verify(queueService).getAssignableQueue("D001", 2);
    }

    @Test
    void getAssignableQueueCountUsesDoctorTypeCategory() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        DoctorSkillMapper doctorSkillMapper = mock(DoctorSkillMapper.class);
        QueueService queueService = mock(QueueService.class);
        TaskSchedulerServiceImpl service = createService(medicalOrderMapper, queueService);
        when(queueService.getAssignableQueueCount(eq("D002"), eq(3)))
                .thenReturn(3L);

        long count = service.getAssignableQueueCount("D002", 3);

        assertThat(count).isEqualTo(3);
        verify(queueService).getAssignableQueueCount("D002", 3);
    }

    @Test
    void getWorkbenchReturnsLabTasksForLaboratoryDoctor() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        QueueService queueService = mock(QueueService.class);
        AgingService agingService = mock(AgingService.class);
        MedicalOrderMapper.DoctorTaskVo task = new MedicalOrderMapper.DoctorTaskVo();
        task.setOrderItemId("MOI_LAB_001");
        task.setItemCode("LAB_BLOOD_001");
        task.setItemName("血常规");
        task.setItemCategory("LAB");
        task.setUrgencyLevel("NORMAL");
        task.setStatus("IN_PROCESS");
        task.setCreateTime(LocalDateTime.of(2026, 7, 10, 9, 0));
        task.setAssignTime(LocalDateTime.of(2026, 7, 10, 9, 5));
        when(medicalOrderMapper.selectDoctorTasks("D003", "LAB"))
                .thenReturn(List.of(task));
        when(agingService.isAgingThresholdReached(task.getCreateTime()))
                .thenReturn(false);
        DoctorTaskService doctorTaskService = new DoctorTaskServiceImpl(
                medicalOrderMapper,
                mock(OrderItemService.class),
                agingService);
        TaskSchedulerServiceImpl service = createService(
                medicalOrderMapper,
                queueService,
                doctorTaskService);

        List<DoctorTaskVo> tasks = service.getDoctorWorkbench("D003", 3);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getItemCategory()).isEqualTo("LAB");
        assertThat(tasks.get(0).getStatus()).isEqualTo("IN_PROCESS");
        assertThat(tasks.get(0).getAssignTime()).isEqualTo("2026-07-10T09:05");
        verify(medicalOrderMapper).selectDoctorTasks("D003", "LAB");
    }

    @Test
    void enqueueByPaymentMarksOrderPaidAndQueuesMainOrderAndItems() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        QueueService queueService = mock(QueueService.class);
        TaskSchedulerServiceImpl service = createService(medicalOrderMapper, queueService);
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
        TaskSchedulerServiceImpl service = createService(medicalOrderMapper, queueService);
        when(medicalOrderMapper.updatePayStatus("MO001")).thenReturn(0);
        when(medicalOrderMapper.enqueueOrder("MO001")).thenReturn(1);
        when(medicalOrderMapper.enqueueOrderItems("MO001")).thenReturn(2);

        service.enqueueByPayment("MO001");

        verify(medicalOrderMapper).updatePayStatus("MO001");
        verify(medicalOrderMapper).enqueueOrder("MO001");
        verify(medicalOrderMapper).enqueueOrderItems("MO001");
    }

    @Test
    void submitReportPublishesMedicalReportAndCompletesTask() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        QueueService queueService = mock(QueueService.class);
        DoctorTaskService doctorTaskService = new DoctorTaskServiceImpl(
                medicalOrderMapper,
                mock(OrderItemService.class),
                mock(AgingService.class));
        TaskSchedulerServiceImpl service = createService(
                medicalOrderMapper,
                queueService,
                doctorTaskService);
        MedicalOrderMapper.DoctorTaskDetailVo task =
                new MedicalOrderMapper.DoctorTaskDetailVo();
        task.setOrderItemId("MOI001");
        task.setPatientId("P001");
        task.setRegisterId("REG001");
        task.setItemCategory("LAB");
        task.setAssignedDoctorId("D003");
        task.setStatus("IN_PROCESS");
        when(medicalOrderMapper.selectTaskDetailById("MOI001")).thenReturn(task);
        when(medicalOrderMapper.insertMedicalReport(any())).thenReturn(1);
        when(medicalOrderMapper.completeTask("MOI001", "D003")).thenReturn(1);

        MedicalReportSubmitRequest request = new MedicalReportSubmitRequest();
        request.setOrderItemId("MOI001");
        request.setResultSummary("白细胞计数在参考范围内。");
        request.setConclusion("血常规未见明显异常。");
        request.setAbnormalFlag("NORMAL");

        MedicalReportVo report = service.submitReport(request, "D003");

        ArgumentCaptor<MedicalReport> reportCaptor = ArgumentCaptor.forClass(MedicalReport.class);
        verify(medicalOrderMapper).insertMedicalReport(reportCaptor.capture());
        verify(medicalOrderMapper).completeTask("MOI001", "D003");
        assertThat(reportCaptor.getValue().getItemCategory()).isEqualTo("LAB");
        assertThat(reportCaptor.getValue().getStatus()).isEqualTo("PUBLISHED");
        assertThat(report.getOrderItemId()).isEqualTo("MOI001");
        assertThat(report.getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void completeTaskRequiresPublishedReport() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        QueueService queueService = mock(QueueService.class);
        OrderItemService orderItemService = mock(OrderItemService.class);
        when(orderItemService.hasPublishedReport("MOI001")).thenReturn(false);
        DoctorTaskService doctorTaskService = new DoctorTaskServiceImpl(
                medicalOrderMapper,
                orderItemService,
                mock(AgingService.class));
        TaskSchedulerServiceImpl service = createService(
                medicalOrderMapper,
                queueService,
                doctorTaskService);

        assertThatThrownBy(() -> service.completeTask("MOI001", "D002"))
                .hasMessageContaining("请先提交并发布检查检验报告");
    }

    private DoctorTaskVo convertToVo(MedicalOrderMapper.QueuedTaskItem item) {
        DoctorTaskVo vo = new DoctorTaskVo();
        vo.setOrderItemId(item.getOrderItemId());
        vo.setItemCode(item.getItemCode());
        vo.setItemName(item.getItemName());
        vo.setItemCategory(item.getItemCategory());
        vo.setUrgencyLevel(item.getUrgencyLevel());
        vo.setStatus(item.getStatus());
        vo.setPatientId(item.getPatientId());
        vo.setRegisterId(item.getRegisterId());
        vo.setPatientName(item.getPatientName());
        return vo;
    }

    private MedicalOrderMapper.QueuedTaskItem queuedItem(
            String orderItemId, String itemCategory) {
        MedicalOrderMapper.QueuedTaskItem item =
                new MedicalOrderMapper.QueuedTaskItem();
        item.setOrderItemId(orderItemId);
        item.setOrderId("MO001");
        item.setItemCode("NEURO_CT_001");
        item.setItemName("Cranial CT");
        item.setItemCategory(itemCategory);
        item.setUrgencyLevel("NORMAL");
        item.setStatus("QUEUED");
        item.setPatientId("P001");
        item.setRegisterId("REG001");
        item.setPatientName("Alice");
        return item;
    }
}
