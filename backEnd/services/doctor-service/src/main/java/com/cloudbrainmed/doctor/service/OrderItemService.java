package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.entity.MedicalOrderItem;

/**
 * 医技项目服务，封装 medical_order_item 的状态变更和查询。
 */
public interface OrderItemService {

    boolean claimTask(String orderItemId, String doctorId);

    boolean updateStatus(String orderItemId, String fromStatus, String toStatus);

    boolean updateStatusDirect(String orderItemId, String status);

    boolean assignDoctor(String orderItemId, String doctorId);

    MedicalOrderItem getById(String orderItemId);

    boolean hasPatientInProgress(String patientId);

    boolean hasDoctorInProgress(String doctorId);

    boolean hasPublishedReport(String orderItemId);
}
