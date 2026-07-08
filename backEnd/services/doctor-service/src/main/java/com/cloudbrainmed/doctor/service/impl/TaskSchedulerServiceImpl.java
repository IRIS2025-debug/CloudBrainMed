package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.service.DoctorTaskService;
import com.cloudbrainmed.doctor.service.QueueService;
import com.cloudbrainmed.doctor.service.SchedulerService;
import com.cloudbrainmed.doctor.service.TaskSchedulerService;
import com.cloudbrainmed.doctor.vo.DoctorTaskDetailVo;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TaskSchedulerServiceImpl implements TaskSchedulerService {

    private final DoctorTaskService doctorTaskService;
    private final QueueService queueService;
    private final SchedulerService schedulerService;
    private final MedicalOrderMapper medicalOrderMapper;

    public TaskSchedulerServiceImpl(
            DoctorTaskService doctorTaskService,
            QueueService queueService,
            SchedulerService schedulerService,
            MedicalOrderMapper medicalOrderMapper) {
        this.doctorTaskService = doctorTaskService;
        this.queueService = queueService;
        this.schedulerService = schedulerService;
        this.medicalOrderMapper = medicalOrderMapper;
    }

    @Override
    public DoctorTaskDetailVo getTaskDetail(
            String orderItemId, String doctorId, Integer doctorType) {
        return doctorTaskService.getTaskDetail(orderItemId, doctorId, doctorType);
    }

    @Override
    @Transactional
    public void startTask(String orderItemId, String doctorId, Integer doctorType) {
        doctorTaskService.startTask(orderItemId, doctorId, doctorType);
    }

    @Override
    @Transactional
    public void completeTask(String orderItemId, String doctorId, Integer doctorType) {
        doctorTaskService.completeTask(orderItemId, doctorId, doctorType);
    }

    @Override
    @Transactional
    public MedicalReportVo submitReport(
            MedicalReportSubmitRequest request, String doctorId, Integer doctorType) {
        return doctorTaskService.submitReport(request, doctorId, doctorType);
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
        log.info("Order {} paid, main order queued={}, {} items enqueued", orderId, queuedOrder, enqueued);
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
}
