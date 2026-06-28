package com.cloudbrainmed.doctor.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class DoctorWorkRule {
    private String ruleId;
    private String doctorId;
    private String deptId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxPatients;
    private Integer preferredLevel;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
