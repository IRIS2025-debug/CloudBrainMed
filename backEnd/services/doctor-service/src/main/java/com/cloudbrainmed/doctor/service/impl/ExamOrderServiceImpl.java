package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.entity.ExamOrder;
import com.cloudbrainmed.doctor.mapper.ExamOrderMapper;
import com.cloudbrainmed.doctor.service.ExamOrderService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamOrderServiceImpl implements ExamOrderService {

    private final ExamOrderMapper examOrderMapper;

    public ExamOrderServiceImpl(ExamOrderMapper examOrderMapper) {
        this.examOrderMapper = examOrderMapper;
    }

    @Override
    public List<ExamOrder> getByRegisterId(String registerId) {
        return examOrderMapper.selectByRegisterId(registerId);
    }

    @Override
    public List<ExamOrder> getByDoctorId(String doctorId) {
        return examOrderMapper.selectByDoctorId(doctorId);
    }
}
