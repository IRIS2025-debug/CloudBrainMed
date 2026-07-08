package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.AiScheduleConflictCheckRequest;
import com.cloudbrainmed.ai.dto.AiScheduleGenerateRequest;
import com.cloudbrainmed.ai.dto.AiScheduleGenerateResponse;
import com.cloudbrainmed.ai.dto.AiSchedulePublishRequest;
import com.cloudbrainmed.ai.dto.AiSchedulePublishResponse;

public interface AiScheduleService {

    AiScheduleGenerateResponse preview(
            AiScheduleGenerateRequest request, String adminId, String adminToken);

    AiScheduleGenerateResponse checkConflicts(
            AiScheduleConflictCheckRequest request, String adminToken);

    AiSchedulePublishResponse publish(
            AiSchedulePublishRequest request, String adminId, String adminToken);
}
