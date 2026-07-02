package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.AiAssistantChatRequest;
import com.cloudbrainmed.ai.dto.PrescriptionDraftResponse;

public interface AiPrescriptionDraftService {

    PrescriptionDraftResponse generate(
            AiAssistantChatRequest request, String doctorId);
}
