package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.vo.DoctorTaskDetailVo;
import com.cloudbrainmed.doctor.vo.DoctorTaskVo;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;

import java.util.List;

/**
 * 任务调度服务
 * 负责队列管理、医生匹配、任务分配
 */
public interface TaskSchedulerService {

    /**
     * 医生工作台：获取当前登录医生的任务列表
     */
    List<DoctorTaskVo> getDoctorWorkbench(String doctorId, Integer doctorType);

    /**
     * 获取当前检查/检验医生可领取的排队任务
     */
    List<DoctorTaskVo> getAssignableQueue(String doctorId, Integer doctorType);

    /**
     * 获取任务详情
     */
    DoctorTaskDetailVo getTaskDetail(String orderItemId, String doctorId);

    /**
     * 医生开始处理任务
     */
    void startTask(String orderItemId, String doctorId);

    /**
     * 医生完成任务
     */
    void completeTask(String orderItemId, String doctorId);

    MedicalReportVo submitReport(MedicalReportSubmitRequest request, String doctorId);

    /**v
     * 支付成功后触发入队
     * 将 waiting_assign 状态转为 queued
     */
    void enqueueByPayment(String orderId);

    /**
     * 执行调度：扫描 queued 队列，匹配医生，分配任务
     * 返回本次分配的任务数
     */
    int runScheduler();

    /**
     * 获取排队中的任务数量
     */
    long getQueueCount();

    /**
     * 获取当前检查/检验医生可领取的排队任务数量
     */
    long getAssignableQueueCount(String doctorId, Integer doctorType);

    /**
     * 医生跳过任务
     */
    void skipTask(String orderItemId, String doctorId);
}