package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.AiFeedbackRequest;

public interface AiReportService {

    boolean saveFeedback(AiFeedbackRequest request, String doctorId);
}
