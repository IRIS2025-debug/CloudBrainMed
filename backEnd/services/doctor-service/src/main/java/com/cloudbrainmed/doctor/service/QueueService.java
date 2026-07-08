package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper.QueuedTaskItem;

import java.util.List;
import java.util.Set;

/**
 * 医技任务队列服务，用于内部自动调度。
 */
public interface QueueService {

    List<QueuedTaskItem> getQueuedTasks(int limit);

    long getQueueCount();

    String findAvailableDoctor(QueuedTaskItem task, Set<String> busyDoctors);

    String itemCategoryForDoctorType(Integer doctorType);
}
