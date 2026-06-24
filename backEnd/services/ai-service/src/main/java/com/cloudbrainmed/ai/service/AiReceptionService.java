package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.AiFeedbackRequest;

public interface AiReceptionService {

    boolean saveFeedback(AiFeedbackRequest request, String doctorId);
}
