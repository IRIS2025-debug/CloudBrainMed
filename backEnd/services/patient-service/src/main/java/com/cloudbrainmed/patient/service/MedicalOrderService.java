// src/main/java/com/cloudbrainmed/patient/service/MedicalOrderService.java
package com.cloudbrainmed.patient.service;

import com.cloudbrainmed.patient.entity.MedicalOrder;
import com.cloudbrainmed.patient.vo.MedicalOrderGroupVo;
import com.cloudbrainmed.patient.vo.ReportDetailVo;

import java.util.List;

public interface MedicalOrderService {

    List<MedicalOrderGroupVo> getGroupedOrdersByPatient(String patientId);

    ReportDetailVo getReportDetail(String orderItemId);

    /**
     * 根据医疗订单ID查询支付状态（供 payment-service Feign 调用）
     * @param orderId 医疗订单ID
     * @return WAITING(待支付) / PAID(已支付) / CANCELLED(已取消) / REFUNDED(已退款)
     */
    String getPayStatus(String orderId);

    /**
     * 根据orderId查询医疗订单（供内部Feign调用）
     */
    MedicalOrder getMedicalOrder(String orderId);


}