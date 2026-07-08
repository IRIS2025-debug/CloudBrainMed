package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.vo.DoctorTaskDetailVo;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;

/**
 * 检查/检验医生任务处理服务。
 */
public interface DoctorTaskService {

    DoctorTaskDetailVo getTaskDetail(
            String orderItemId, String doctorId, Integer doctorType);

    void startTask(String orderItemId, String doctorId, Integer doctorType);

    void completeTask(String orderItemId, String doctorId, Integer doctorType);

    MedicalReportVo submitReport(
            MedicalReportSubmitRequest request, String doctorId, Integer doctorType);
}
