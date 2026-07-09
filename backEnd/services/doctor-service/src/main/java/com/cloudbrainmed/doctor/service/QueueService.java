package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper.QueuedTaskItem;
import com.cloudbrainmed.doctor.vo.DoctorTaskVo;

import java.util.List;

/**
 * 队列服务
 * 负责排队任务查询、数量统计、医生技能匹配
 */
public interface QueueService {

    /**
     * 获取当前医生可领取的排队任务（按优先级排序）
     * @param doctorId 医生ID，用于根据其技能（doctor_skill）过滤
     * @param doctorType 医生类型，用于降级过滤（无技能配置时按类别过滤）
     */
    List<DoctorTaskVo> getAssignableQueue(String doctorId, Integer doctorType);

    /**
     * 获取排队任务原始数据（用于调度器）
     */
    List<QueuedTaskItem> getQueuedTasks(int limit);

    /**
     * 获取排队任务数量
     */
    long getQueueCount();

    /**
     * 获取当前医生可领取的排队任务数量
     * @param doctorId 医生ID，用于根据其技能过滤
     * @param doctorType 医生类型，用于降级过滤
     */
    long getAssignableQueueCount(String doctorId, Integer doctorType);

    /**
     * 匹配可处理该任务的医生
     */
    String findAvailableDoctor(QueuedTaskItem task, java.util.Set<String> busyDoctors);

    /**
     * 根据医生类型获取对应的项目类别
     */
    String itemCategoryForDoctorType(Integer doctorType);
}