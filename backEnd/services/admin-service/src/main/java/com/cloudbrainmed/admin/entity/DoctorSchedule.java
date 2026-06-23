package com.cloudbrainmed.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
@TableName("doctor_schedule")
public class DoctorSchedule {

    @TableId(type = IdType.INPUT)
    private String scheduleId;

    private String planId;
    private String doctorId;
    private String doctorName;
    private String deptId;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxNum;
    private Integer remainNum;
    private Integer status;
    private BigDecimal price;
    private String room;
    private String sourceType;
    private String scheduleStatus;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;
}