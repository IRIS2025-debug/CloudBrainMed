package com.cloudbrainmed.admin.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ScheduleUpdateDto {
    private String scheduleId;
    private String doctorId;
    private String doctorName;
    private String deptId;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxNum;
    private Integer remainNum;
    private BigDecimal price;
    private String room;
    private Integer status;
}