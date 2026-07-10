package com.cloudbrainmed.admin.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SchedulePublishFailure {

    private int index;
    private String doctorId;
    private String doctorName;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private String reason;
    private String conflictType;
}
