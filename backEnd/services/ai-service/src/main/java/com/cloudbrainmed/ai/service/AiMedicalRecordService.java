package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.AiRecordGenerateRequest;
import com.cloudbrainmed.ai.dto.AiRecordGenerateResponse;

public interface AiMedicalRecordService {

    AiRecordGenerateResponse generate(
            AiRecordGenerateRequest request, String doctorId);
}
