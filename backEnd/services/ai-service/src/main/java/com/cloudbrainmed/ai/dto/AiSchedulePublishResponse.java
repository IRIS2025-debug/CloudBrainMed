package com.cloudbrainmed.ai.dto;

import com.cloudbrainmed.admin.entity.DoctorSchedule;
import com.cloudbrainmed.admin.dto.SchedulePublishFailure;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiSchedulePublishResponse {

    private String traceId;
    private String status;
    private int submittedCount;
    private int createdCount;
    private List<DoctorSchedule> createdSchedules = new ArrayList<>();
    private List<SchedulePublishFailure> failedItems = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
