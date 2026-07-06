package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.entity.MedicalOrderItem;

/**
 * 医嘱项目服务
 * 负责 medical_order_item 的原子状态变更、查询等操作
 */
public interface OrderItemService {

    /**
     * 原子认领任务（乐观锁：WHERE status = 'QUEUED'）
     */
    boolean claimTask(String orderItemId, String doctorId);

    /**
     * 更新项目状态
     */
    boolean updateStatus(String orderItemId, String fromStatus, String toStatus);

    /**
     * 直接更新项目状态（不校验旧状态）
     */
    boolean updateStatusDirect(String orderItemId, String status);

    /**
     * 分配医生到项目
     */
    boolean assignDoctor(String orderItemId, String doctorId);

    /**
     * 根据ID查询项目
     */
    MedicalOrderItem getById(String orderItemId);

    /**
     * 检查患者是否有正在处理的项目
     */
    boolean hasPatientInProgress(String patientId);

    /**
     * 检查医生是否有正在处理的项目
     */
    boolean hasDoctorInProgress(String doctorId);

    /**
     * 检查项目是否已发布报告
     */
    boolean hasPublishedReport(String orderItemId);

    /**
     * 释放任务：将 IN_PROCESS 回退到 QUEUED，清除医生分配
     * 用于医生跳过任务
     */
    boolean releaseTask(String orderItemId, String doctorId);
}