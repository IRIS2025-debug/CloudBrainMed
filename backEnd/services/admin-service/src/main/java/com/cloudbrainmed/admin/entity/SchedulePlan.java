package com.cloudbrainmed.admin.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SchedulePlan {
    private String planId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String deptId;
    private String strategy;
    private String status;
    private BigDecimal coverageRate;
    private Integer conflictCount;
    private String createdBy;
    private LocalDateTime createTime;
    private LocalDateTime publishTime;
}
