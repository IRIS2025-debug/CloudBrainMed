package com.cloudbrainmed.patient.service;

import com.cloudbrainmed.patient.entity.Prescription;
import com.cloudbrainmed.patient.vo.RegisterPrescriptionGroupVo;

import java.util.List;

public interface PrescriptionService {
    List<Prescription> getByRegisterId(String registerId, String patientId);
    List<Prescription> getByPatientId(String patientId);

    /**
     * 获取按挂号分组的处方列表
     */
    List<RegisterPrescriptionGroupVo> getGroupedByPatientId(String patientId);
}
