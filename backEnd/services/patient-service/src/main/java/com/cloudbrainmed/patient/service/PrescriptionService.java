package com.cloudbrainmed.patient.service;

import com.cloudbrainmed.patient.entity.Prescription;
import com.cloudbrainmed.patient.vo.RegisterPrescriptionGroupVo;

import java.util.List;

public interface PrescriptionService {
    List<Prescription> getByRegisterId(String registerId, String patientId);
    List<Prescription> getByPatientId(String patientId);

    /**
     * 获取按挂号分组的处方列表
     */
    List<RegisterPrescriptionGroupVo> getGroupedByPatientId(String patientId);

    /**
     * 根据处方ID查询支付状态（供 payment-service Feign 调用）
     * @param prescriptionId 处方ID
     * @return WAITING(待支付) / PAID(已支付) / CANCELLED(已取消) / REFUNDED(已退款)
     */
    String getPayStatus(String prescriptionId);

    /**
     * 根据prescriptionId查询处方（供内部Feign调用）
     */
    Prescription getPrescription(String prescriptionId);
}
