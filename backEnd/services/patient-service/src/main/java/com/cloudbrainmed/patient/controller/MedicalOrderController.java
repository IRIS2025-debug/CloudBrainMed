// src/main/java/com/cloudbrainmed/patient/controller/MedicalOrderController.java
package com.cloudbrainmed.patient.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.patient.service.MedicalOrderService;
import com.cloudbrainmed.patient.vo.MedicalOrderGroupVo;
import com.cloudbrainmed.patient.vo.ReportDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patient-service/medical-order")
public class MedicalOrderController {

    @Autowired
    private MedicalOrderService medicalOrderService;

    /**
     * 获取患者按挂号分组的医技申请列表（检查报告列表）
     */
    @GetMapping("/grouped/{patientId}")
    public Result<List<MedicalOrderGroupVo>> getGroupedOrders(@PathVariable String patientId) {
        List<MedicalOrderGroupVo> groups = medicalOrderService.getGroupedOrdersByPatient(patientId);
        return Result.success(groups);
    }

    /**
     * 获取报告详情
     */
    @GetMapping("/report/{orderItemId}")
    public Result<ReportDetailVo> getReportDetail(@PathVariable String orderItemId) {
        ReportDetailVo detail = medicalOrderService.getReportDetail(orderItemId);
        return Result.success(detail);
    }
}