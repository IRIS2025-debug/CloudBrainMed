package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.PrescriptionReviewRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewResponse;

public interface AiPrescriptionReviewService {

    PrescriptionReviewResponse review(
            PrescriptionReviewRequest request, String doctorId);
}
