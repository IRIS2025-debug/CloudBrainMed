package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.AiScheduleConflictCheckRequest;
import com.cloudbrainmed.ai.dto.AiScheduleGenerateRequest;
import com.cloudbrainmed.ai.dto.AiScheduleGenerateResponse;
import com.cloudbrainmed.ai.dto.AiSchedulePublishRequest;
import com.cloudbrainmed.ai.dto.AiSchedulePublishResponse;

public interface AiScheduleService {

    AiScheduleGenerateResponse preview(
            AiScheduleGenerateRequest request, String adminId);

    AiScheduleGenerateResponse checkConflicts(
            AiScheduleConflictCheckRequest request);

    AiSchedulePublishResponse publish(
            AiSchedulePublishRequest request, String adminId);
}
