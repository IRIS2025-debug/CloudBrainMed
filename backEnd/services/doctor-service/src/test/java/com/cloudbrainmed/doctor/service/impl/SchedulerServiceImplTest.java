package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper.QueuedTaskItem;
import com.cloudbrainmed.doctor.service.AgingService;
import com.cloudbrainmed.doctor.service.OrderItemService;
import com.cloudbrainmed.doctor.service.QueueService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchedulerServiceImplTest {

    @Test
    void schedulerAssignsQueuedTaskWhenPatientAlreadyHasAnotherInProgressItem() {
        QueueService queueService = mock(QueueService.class);
        OrderItemService orderItemService = mock(OrderItemService.class);
        AgingService agingService = mock(AgingService.class);
        SchedulerServiceImpl service = new SchedulerServiceImpl(
                queueService, orderItemService, agingService);
        QueuedTaskItem task = queuedTask("MOI002", "P001");
        when(queueService.getQueuedTasks(100)).thenReturn(List.of(task));
        when(orderItemService.hasPatientInProgress("P001")).thenReturn(true);
        when(queueService.findAvailableDoctor(eq(task), any(Set.class)))
                .thenReturn("D003");
        when(orderItemService.updateStatus("MOI002", "QUEUED", "IN_PROCESS"))
                .thenReturn(true);

        int assigned = service.runScheduler();

        assertThat(assigned).isEqualTo(1);
        verify(orderItemService).assignDoctor("MOI002", "D003");
    }

    private QueuedTaskItem queuedTask(String orderItemId, String patientId) {
        QueuedTaskItem task = new QueuedTaskItem();
        task.setOrderItemId(orderItemId);
        task.setPatientId(patientId);
        return task;
    }
}
