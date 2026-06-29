package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.entity.ExamOrder;

import java.util.List;

public interface ExamOrderService {
    List<ExamOrder> getByRegisterId(String registerId);
    List<ExamOrder> getByDoctorId(String doctorId);
}
