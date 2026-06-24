package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmRequest;
import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmResponse;

public interface MedicalOrderService {
    MedicalOrderConfirmResponse confirm(
            MedicalOrderConfirmRequest request, String doctorId);
}
