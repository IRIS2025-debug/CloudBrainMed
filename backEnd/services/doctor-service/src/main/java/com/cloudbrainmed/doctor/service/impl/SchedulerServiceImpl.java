package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper.QueuedTaskItem;
import com.cloudbrainmed.doctor.service.AgingService;
import com.cloudbrainmed.doctor.service.OrderItemService;
import com.cloudbrainmed.doctor.service.QueueService;
import com.cloudbrainmed.doctor.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class SchedulerServiceImpl implements SchedulerService {

    private final QueueService queueService;
    private final OrderItemService orderItemService;
    private final AgingService agingService;

    private static final int BATCH_SIZE = 100;

    public SchedulerServiceImpl(
            QueueService queueService,
            OrderItemService orderItemService,
            AgingService agingService) {
        this.queueService = queueService;
        this.orderItemService = orderItemService;
        this.agingService = agingService;
    }

    @Override
    @Transactional
    public int runScheduler() {
        List<QueuedTaskItem> queuedTasks = queueService.getQueuedTasks(BATCH_SIZE);
        if (queuedTasks.isEmpty()) {
            return 0;
        }

        int assigned = 0;
        Set<String> busyDoctors = new HashSet<>();

        for (QueuedTaskItem task : queuedTasks) {
            // Schedule by order item; another in-progress item for the same patient should not block this one.
            String assignedDoctor = queueService.findAvailableDoctor(task, busyDoctors);
            if (assignedDoctor == null) {
                continue;
            }

            boolean claimed = orderItemService.updateStatus(
                    task.getOrderItemId(), "QUEUED", "IN_PROCESS");
            if (!claimed) {
                continue;
            }
            orderItemService.assignDoctor(task.getOrderItemId(), assignedDoctor);
            busyDoctors.add(assignedDoctor);
            assigned++;
            log.info("Scheduler: task {} assigned to doctor {} for patient {}",
                    task.getOrderItemId(), assignedDoctor, task.getPatientId());
        }

        return assigned;
    }
}
