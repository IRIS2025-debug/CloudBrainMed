package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.entity.MedicalOrderItem;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.service.OrderItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderItemServiceImpl implements OrderItemService {

    private final MedicalOrderMapper medicalOrderMapper;

    public OrderItemServiceImpl(MedicalOrderMapper medicalOrderMapper) {
        this.medicalOrderMapper = medicalOrderMapper;
    }

    @Override
    @Transactional
    public boolean claimTask(String orderItemId, String doctorId) {
        return medicalOrderMapper.claimTaskAtomically(orderItemId, doctorId) > 0;
    }

    @Override
    @Transactional
    public boolean updateStatus(String orderItemId, String fromStatus, String toStatus) {
        return medicalOrderMapper.updateItemStatus(orderItemId, fromStatus, toStatus) > 0;
    }

    @Override
    @Transactional
    public boolean updateStatusDirect(String orderItemId, String status) {
        return medicalOrderMapper.updateItemStatusDirect(orderItemId, status) > 0;
    }

    @Override
    @Transactional
    public boolean assignDoctor(String orderItemId, String doctorId) {
        return medicalOrderMapper.assignDoctor(orderItemId, doctorId) > 0;
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
}
