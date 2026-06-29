package com.cloudbrainmed.admin.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class SchedulePlanItem {
    private String planItemId;
    private String planId;
    private String doctorId;
    private String doctorName;
    private String deptId;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxNum;
    private BigDecimal price;
    private String room;
    private BigDecimal score;
    private Integer conflictFlag;
    private String conflictReason;
    private LocalDateTime createTime;
}
