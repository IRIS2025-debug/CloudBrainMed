package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.dto.PrescriptionCreateDto;
import com.cloudbrainmed.doctor.entity.Prescription;
import java.util.List;

public interface PrescriptionService {
    Prescription create(PrescriptionCreateDto dto, String doctorId, String doctorName);
    List<Prescription> getByRegisterId(String registerId);
    Prescription getById(String prescriptionId);
}
