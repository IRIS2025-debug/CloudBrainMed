package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
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

/**
 * 调度器服务实现
 * 扫描 QUEUED 队列，匹配医生，自动分配
 */
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
        // 1. 获取排队任务（已按优先级+老化排序）
        List<QueuedTaskItem> queuedTasks = queueService.getQueuedTasks(BATCH_SIZE);
        if (queuedTasks.isEmpty()) {
            return 0;
        }

        int assigned = 0;
        Set<String> busyPatients = new HashSet<>();
        Set<String> busyDoctors = new HashSet<>();

        // 2. 遍历排队任务，尝试分配
        for (QueuedTaskItem task : queuedTasks) {
            // 跳过同患者已有在处理任务的
            if (busyPatients.contains(task.getPatientId())) {
                continue;
            }

            // 检查患者是否已有处理中的项目
            if (orderItemService.hasPatientInProgress(task.getPatientId())) {
                busyPatients.add(task.getPatientId());
                continue;
            }

            // 3. 匹配可用医生
            String assignedDoctor = queueService.findAvailableDoctor(task, busyDoctors);
            if (assignedDoctor == null) {
                continue;
            }

            // 4. 原子分配
            boolean claimed = orderItemService.updateStatus(
                    task.getOrderItemId(), "QUEUED", "IN_PROCESS");
            if (!claimed) {
                continue;
            }
            orderItemService.assignDoctor(task.getOrderItemId(), assignedDoctor);
            busyPatients.add(task.getPatientId());
            busyDoctors.add(assignedDoctor);
            assigned++;
            log.info("Scheduler: task {} assigned to doctor {} for patient {}",
                    task.getOrderItemId(), assignedDoctor, task.getPatientId());
        }

        return assigned;
    }
}
