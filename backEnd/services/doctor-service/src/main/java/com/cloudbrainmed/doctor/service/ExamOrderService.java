package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.dto.SaveReportRequestDto;
import com.cloudbrainmed.doctor.entity.ExamOrder;
import com.cloudbrainmed.doctor.entity.MedicalReport;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ExamOrderService {
    List<ExamOrder> getByRegisterId(String registerId, String doctorId);
    List<ExamOrder> getByDoctorId(String doctorId);
    /**
     * 保存CT检查报告
     */
   @Transactional(rollbackFor = Exception.class)
    MedicalReport saveReport(SaveReportRequestDto request);
}
