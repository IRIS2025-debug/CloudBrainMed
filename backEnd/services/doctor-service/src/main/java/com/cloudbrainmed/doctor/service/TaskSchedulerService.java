package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.vo.DoctorTaskDetailVo;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;

/**
 * 检查/检验任务调度与处理服务。
 */
public interface TaskSchedulerService {

    DoctorTaskDetailVo getTaskDetail(
            String orderItemId, String doctorId, Integer doctorType);

    void startTask(String orderItemId, String doctorId, Integer doctorType);

    void completeTask(String orderItemId, String doctorId, Integer doctorType);

    MedicalReportVo submitReport(
            MedicalReportSubmitRequest request, String doctorId, Integer doctorType);

    void enqueueByPayment(String orderId);

    int runScheduler();

    long getQueueCount();
}
