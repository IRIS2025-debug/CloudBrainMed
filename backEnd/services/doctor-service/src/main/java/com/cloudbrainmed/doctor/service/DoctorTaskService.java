package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.vo.DoctorTaskDetailVo;
import com.cloudbrainmed.doctor.vo.DoctorTaskVo;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;

import java.util.List;

/**
 * 医生任务服务
 * 负责医生工作台、任务领取、完成任务等医生端操作
 */
public interface DoctorTaskService {

    /**
     * 获取医生工作台任务列表（当前医生的 IN_PROCESS 任务）
     */
    List<DoctorTaskVo> getDoctorWorkbench(String doctorId, Integer doctorType);

    /**
     * 获取任务详情
     */
    DoctorTaskDetailVo getTaskDetail(String orderItemId, String doctorId);

    /**
     * 医生开始处理任务（原子认领，乐观锁）
     */
    void startTask(String orderItemId, String doctorId);

    /**
     * 医生完成任务
     */
    void completeTask(String orderItemId, String doctorId);

    /**
     * 提交检查/检验报告
     */
    MedicalReportVo submitReport(MedicalReportSubmitRequest request, String doctorId);

    /**
     * 医生跳过任务：将 IN_PROCESS 状态的任务回退到 QUEUED
     * 被跳过的任务重新排队，通过老化机制获得更高优先级
     */
    void skipTask(String orderItemId, String doctorId);
}