package com.cloudbrainmed.patient.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
@TableName("doctor_schedule")
public class DoctorSchedule {
    @TableId(type = IdType.ASSIGN_ID)
    private String scheduleId;
    private String planId;
    private String doctorId;
    private String doctorName;
    private String deptId;
    private String workDate;  // 使用String，前端传入日期
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxNum;
    private Integer remainNum;
    private Integer status;
    private BigDecimal price;
    private String room;
    private String sourceType;
    private String scheduleStatus;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updateTime;
    private String adjustReason;
}