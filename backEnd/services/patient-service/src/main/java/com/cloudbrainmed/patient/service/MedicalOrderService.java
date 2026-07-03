// src/main/java/com/cloudbrainmed/patient/service/MedicalOrderService.java
package com.cloudbrainmed.patient.service;

import com.cloudbrainmed.patient.vo.MedicalOrderGroupVo;
import com.cloudbrainmed.patient.vo.ReportDetailVo;

import java.util.List;

public interface MedicalOrderService {

    List<MedicalOrderGroupVo> getGroupedOrdersByPatient(String patientId);

    ReportDetailVo getReportDetail(String orderItemId);
}