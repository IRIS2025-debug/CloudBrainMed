package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.mapper.DoctorSkillMapper;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;
import com.cloudbrainmed.doctor.vo.DoctorTaskVo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskSchedulerServiceImplTest {

    @Test
    void getAssignableQueueReturnsExamTasksForExamDoctor() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        DoctorSkillMapper doctorSkillMapper = mock(DoctorSkillMapper.class);
        TaskSchedulerServiceImpl service =
                new TaskSchedulerServiceImpl(medicalOrderMapper, doctorSkillMapper);
        MedicalOrderMapper.QueuedTaskItem item = queuedItem("ITEM001", "EXAM");
        when(medicalOrderMapper.findQueuedTasksByCategory("EXAM", 100))
                .thenReturn(List.of(item));

        List<DoctorTaskVo> tasks = service.getAssignableQueue(2);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getOrderItemId()).isEqualTo("ITEM001");
        assertThat(tasks.get(0).getItemCategory()).isEqualTo("EXAM");
        verify(medicalOrderMapper).findQueuedTasksByCategory("EXAM", 100);
    }

    @Test
    void getAssignableQueueCountUsesDoctorTypeCategory() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        DoctorSkillMapper doctorSkillMapper = mock(DoctorSkillMapper.class);
        TaskSchedulerServiceImpl service =
                new TaskSchedulerServiceImpl(medicalOrderMapper, doctorSkillMapper);
        when(medicalOrderMapper.countQueuedTasksByCategory("LAB"))
                .thenReturn(3L);

        long count = service.getAssignableQueueCount(3);

        assertThat(count).isEqualTo(3);
        verify(medicalOrderMapper).countQueuedTasksByCategory("LAB");
    }

    @Test
    void enqueueByPaymentMarksOrderPaidAndQueuesMainOrderAndItems() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        DoctorSkillMapper doctorSkillMapper = mock(DoctorSkillMapper.class);
        TaskSchedulerServiceImpl service =
                new TaskSchedulerServiceImpl(medicalOrderMapper, doctorSkillMapper);
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
        DoctorSkillMapper doctorSkillMapper = mock(DoctorSkillMapper.class);
        TaskSchedulerServiceImpl service =
                new TaskSchedulerServiceImpl(medicalOrderMapper, doctorSkillMapper);
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
        DoctorSkillMapper doctorSkillMapper = mock(DoctorSkillMapper.class);
        TaskSchedulerServiceImpl service =
                new TaskSchedulerServiceImpl(medicalOrderMapper, doctorSkillMapper);
        MedicalOrderMapper.DoctorTaskDetailVo task =
                new MedicalOrderMapper.DoctorTaskDetailVo();
        task.setOrderItemId("MOI001");
        task.setPatientId("P001");
        task.setRegisterId("REG001");
        task.setItemCategory("EXAM");
        task.setAssignedDoctorId("D002");
        task.setStatus("IN_PROCESS");
        when(medicalOrderMapper.selectTaskDetailById("MOI001")).thenReturn(task);
        when(medicalOrderMapper.insertMedicalReport(any())).thenReturn(1);
        when(medicalOrderMapper.completeTask("MOI001", "D002")).thenReturn(1);

        MedicalReportSubmitRequest request = new MedicalReportSubmitRequest();
        request.setOrderItemId("MOI001");
        request.setResultSummary("No acute intracranial abnormality.");
        request.setConclusion("Normal cranial CT.");
        request.setAbnormalFlag("NORMAL");

        MedicalReportVo report = service.submitReport(request, "D002");

        verify(medicalOrderMapper).insertMedicalReport(any());
        verify(medicalOrderMapper).completeTask("MOI001", "D002");
        assertThat(report.getOrderItemId()).isEqualTo("MOI001");
        assertThat(report.getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void completeTaskRequiresPublishedReport() {
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        DoctorSkillMapper doctorSkillMapper = mock(DoctorSkillMapper.class);
        TaskSchedulerServiceImpl service =
                new TaskSchedulerServiceImpl(medicalOrderMapper, doctorSkillMapper);
        when(medicalOrderMapper.countPublishedReportsByOrderItemId("MOI001"))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.completeTask("MOI001", "D002"))
                .hasMessageContaining("请先提交并发布检查检验报告");
    }

    private MedicalOrderMapper.QueuedTaskItem queuedItem(
            String orderItemId, String itemCategory) {
        MedicalOrderMapper.QueuedTaskItem item =
                new MedicalOrderMapper.QueuedTaskItem();
        item.setOrderItemId(orderItemId);
        item.setOrderId("MO001");
        item.setItemCode("CRANIAL_CT_PLAIN");
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
