package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.MedicineChatRequest;

public interface MedicineAiChatService {

    String chat(MedicineChatRequest request);
}
