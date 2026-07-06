package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.service.*;
import com.cloudbrainmed.doctor.vo.DoctorTaskDetailVo;
import com.cloudbrainmed.doctor.vo.DoctorTaskVo;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class TaskSchedulerServiceImpl implements TaskSchedulerService {

    private final DoctorTaskService doctorTaskService;
    private final QueueService queueService;
    private final OrderItemService orderItemService;
    private final SchedulerService schedulerService;
    private final AgingService agingService;
    private final MedicalOrderMapper medicalOrderMapper;

    public TaskSchedulerServiceImpl(
            DoctorTaskService doctorTaskService,
            QueueService queueService,
            OrderItemService orderItemService,
            SchedulerService schedulerService,
            AgingService agingService,
            MedicalOrderMapper medicalOrderMapper) {
        this.doctorTaskService = doctorTaskService;
        this.queueService = queueService;
        this.orderItemService = orderItemService;
        this.schedulerService = schedulerService;
        this.agingService = agingService;
        this.medicalOrderMapper = medicalOrderMapper;
    }

    @Override
    public List<DoctorTaskVo> getDoctorWorkbench(String doctorId, Integer doctorType) {
        return doctorTaskService.getDoctorWorkbench(doctorId, doctorType);
    }

    @Override
    public List<DoctorTaskVo> getAssignableQueue(String doctorId, Integer doctorType) {
        return queueService.getAssignableQueue(doctorId, doctorType);
    }

    @Override
    public DoctorTaskDetailVo getTaskDetail(String orderItemId, String doctorId) {
        return doctorTaskService.getTaskDetail(orderItemId, doctorId);
    }

    @Override
    @Transactional
    public void startTask(String orderItemId, String doctorId) {
        doctorTaskService.startTask(orderItemId, doctorId);
    }

    @Override
    @Transactional
    public void completeTask(String orderItemId, String doctorId) {
        doctorTaskService.completeTask(orderItemId, doctorId);
    }

    @Override
    @Transactional
    public MedicalReportVo submitReport(MedicalReportSubmitRequest request, String doctorId) {
        return doctorTaskService.submitReport(request, doctorId);
    }

    @Override
    @Transactional
    public void enqueueByPayment(String orderId) {
        int paid = medicalOrderMapper.updatePayStatus(orderId);
        if (paid == 0) {
            log.info("Payment callback: order {} was already marked paid or not found", orderId);
        }
        int queuedOrder = medicalOrderMapper.enqueueOrder(orderId);
        int enqueued = medicalOrderMapper.enqueueOrderItems(orderId);
        log.info("Order {} paid, main order queued={}, {} items enqueued",
                orderId, queuedOrder, enqueued);
    }

    @Override
    @Transactional
    public int runScheduler() {
        return schedulerService.runScheduler();
    }

    @Override
    public long getQueueCount() {
        return queueService.getQueueCount();
    }

    @Override
    public long getAssignableQueueCount(String doctorId, Integer doctorType) {
        return queueService.getAssignableQueueCount(doctorId, doctorType);
    }

    @Override
    @Transactional
    public void skipTask(String orderItemId, String doctorId) {
        doctorTaskService.skipTask(orderItemId, doctorId);
    }
}