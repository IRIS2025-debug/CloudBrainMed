package com.cloudbrainmed.patient.service;

import com.cloudbrainmed.patient.entity.RegisterReport;

import java.util.List;

public interface RegisterReportService {
    List<RegisterReport> getByRegisterId(String registerId, String patientId);
    List<RegisterReport> getByPatientId(String patientId);
}
