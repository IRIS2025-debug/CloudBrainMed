package com.cloudbrainmed.patient.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class ScheduleVo {
    private String scheduleId;
    private String doctorId;
    private String doctorName;
    private String workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxNum;
    private Integer remainNum;
    private Integer status;
    private BigDecimal price;
    private String room;
}