package com.cloudbrainmed.ai.dto;

import com.cloudbrainmed.admin.entity.DoctorSchedule;
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
    private List<String> warnings = new ArrayList<>();
}
