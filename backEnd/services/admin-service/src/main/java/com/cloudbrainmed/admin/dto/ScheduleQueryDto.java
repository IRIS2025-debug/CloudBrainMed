package com.cloudbrainmed.admin.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ScheduleQueryDto {
    private String doctorId;
    private String deptId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer page = 1;
    private Integer limit = 20;
}