package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.entity.MedicalOrderItem;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.service.DoctorScheduleService;
import com.cloudbrainmed.doctor.service.OrderItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 医嘱项目服务实现
 * 封装对 medical_order_item 表的所有原子操作
 */
@Slf4j
@Service
public class OrderItemServiceImpl implements OrderItemService {

    private final MedicalOrderMapper medicalOrderMapper;
    private final DoctorScheduleService doctorScheduleService;

    public OrderItemServiceImpl(MedicalOrderMapper medicalOrderMapper,
                                DoctorScheduleService doctorScheduleService) {
        this.medicalOrderMapper = medicalOrderMapper;
        this.doctorScheduleService = doctorScheduleService;
    }

    @Override
    @Transactional
    public boolean claimTask(String orderItemId, String doctorId) {
        log.info("OrderItem claimTask invoked, orderItemId={}, doctorId={}", orderItemId, doctorId);
        ensureDoctorInSchedule(doctorId, orderItemId);
        int updated = medicalOrderMapper.claimTaskAtomically(orderItemId, doctorId);
        log.info("OrderItem claimTask DB updated, orderItemId={}, doctorId={}, updated={}", orderItemId, doctorId, updated);
        return updated > 0;
    }

    @Override
    @Transactional
    public boolean updateStatus(String orderItemId, String fromStatus, String toStatus) {
        int updated = medicalOrderMapper.updateItemStatus(orderItemId, fromStatus, toStatus);
        return updated > 0;
    }

    @Override
    @Transactional
    public boolean updateStatusDirect(String orderItemId, String status) {
        int updated = medicalOrderMapper.updateItemStatusDirect(orderItemId, status);
        return updated > 0;
    }

    @Override
    @Transactional
    public boolean assignDoctor(String orderItemId, String doctorId) {
        log.info("OrderItem assignDoctor invoked, orderItemId={}, doctorId={}", orderItemId, doctorId);
        ensureDoctorInSchedule(doctorId, orderItemId);
        int updated = medicalOrderMapper.assignDoctor(orderItemId, doctorId);
        log.info("OrderItem assignDoctor DB updated, orderItemId={}, doctorId={}, updated={}", orderItemId, doctorId, updated);
        return updated > 0;
    }

    @Override
    public MedicalOrderItem getById(String orderItemId) {
        return medicalOrderMapper.selectOrderItemById(orderItemId);
    }

    @Override
    public boolean hasPatientInProgress(String patientId) {
        return medicalOrderMapper.countInProgressByPatient(patientId) > 0;
    }

    @Override
    public boolean hasDoctorInProgress(String doctorId) {
        return medicalOrderMapper.countInProgressByDoctor(doctorId) > 0;
    }

    @Override
    public boolean hasPublishedReport(String orderItemId) {
        return medicalOrderMapper.countPublishedReportsByOrderItemId(orderItemId) > 0;
    }

    @Override
    @Transactional
    public boolean releaseTask(String orderItemId, String doctorId) {
        log.info("OrderItem releaseTask invoked, orderItemId={}, doctorId={}", orderItemId, doctorId);
        int updated = medicalOrderMapper.releaseTask(orderItemId, doctorId);
        if (updated > 0) {
            log.info("Task {} released by doctor {}, back to QUEUED", orderItemId, doctorId);
        }
        log.info("OrderItem releaseTask DB updated, orderItemId={}, doctorId={}, updated={}", orderItemId, doctorId, updated);
        return updated > 0;
    }

    private void ensureDoctorInSchedule(String doctorId, String orderItemId) {
        if (!doctorScheduleService.isDoctorAvailable(doctorId, LocalDateTime.now())) {
            log.warn("Reject task assignment because doctor is not in schedule, orderItemId={}, doctorId={}",
                    orderItemId, doctorId);
            throw new BusinessException("当前医生不在排班时间内，无法分配检查检验任务");
        }
    }
}