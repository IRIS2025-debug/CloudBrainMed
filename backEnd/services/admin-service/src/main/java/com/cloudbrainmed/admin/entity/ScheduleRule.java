package com.cloudbrainmed.admin.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ScheduleRule {
    private String ruleId;
    private String doctorId;
    private String doctorName;
    private String deptId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxPatients;
    private Integer preferredLevel;
    private LocalDate validFrom;
    private LocalDate validTo;
}
