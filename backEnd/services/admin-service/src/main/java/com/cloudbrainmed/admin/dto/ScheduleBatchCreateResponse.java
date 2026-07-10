package com.cloudbrainmed.admin.dto;

import com.cloudbrainmed.admin.entity.DoctorSchedule;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ScheduleBatchCreateResponse {

    private int submittedCount;
    private int createdCount;
    private List<DoctorSchedule> createdSchedules = new ArrayList<>();
    private List<SchedulePublishFailure> failedItems = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
