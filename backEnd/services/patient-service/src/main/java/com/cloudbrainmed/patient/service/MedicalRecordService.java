package com.cloudbrainmed.patient.service;

import com.cloudbrainmed.patient.entity.MedicalRecord;

import java.util.List;

public interface MedicalRecordService {
    List<MedicalRecord> getByRegisterId(String registerId, String patientId);
    List<MedicalRecord> getByPatientId(String patientId);
}
