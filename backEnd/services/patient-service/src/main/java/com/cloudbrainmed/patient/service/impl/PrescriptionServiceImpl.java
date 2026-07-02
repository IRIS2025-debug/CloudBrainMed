package com.cloudbrainmed.patient.service.impl;

import com.cloudbrainmed.patient.entity.Prescription;
import com.cloudbrainmed.patient.mapper.PrescriptionMapper;
import com.cloudbrainmed.patient.service.PrescriptionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionMapper mapper;

    public PrescriptionServiceImpl(PrescriptionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Prescription> getByRegisterId(String registerId, String patientId) {
        return mapper.selectByRegisterId(registerId, patientId);
    }

    @Override
    public List<Prescription> getByPatientId(String patientId) {
        return mapper.selectByPatientId(patientId);
    }
}
