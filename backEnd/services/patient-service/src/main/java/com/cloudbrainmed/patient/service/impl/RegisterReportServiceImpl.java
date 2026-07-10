package com.cloudbrainmed.patient.service.impl;

import com.cloudbrainmed.patient.entity.RegisterReport;
import com.cloudbrainmed.patient.mapper.RegisterReportMapper;
import com.cloudbrainmed.patient.service.RegisterReportService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegisterReportServiceImpl implements RegisterReportService {

    private final RegisterReportMapper mapper;

    public RegisterReportServiceImpl(RegisterReportMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<RegisterReport> getByRegisterId(String registerId, String patientId) {
        return mapper.selectByRegisterId(registerId, patientId);
    }

    @Override
    public List<RegisterReport> getByPatientId(String patientId) {
        return mapper.selectByPatientId(patientId);
    }
}
