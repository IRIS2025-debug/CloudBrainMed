package com.cloudbrainmed.patient.service.impl;

import com.cloudbrainmed.patient.entity.MedicalRecord;
import com.cloudbrainmed.patient.mapper.MedicalRecordMapper;
import com.cloudbrainmed.patient.service.MedicalRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordMapper mapper;

    public MedicalRecordServiceImpl(MedicalRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<MedicalRecord> getByRegisterId(String registerId, String patientId) {
        return mapper.selectByRegisterId(registerId, patientId);
    }

    @Override
    public List<MedicalRecord> getByPatientId(String patientId) {
        return mapper.selectByPatientId(patientId);
    }
}
