package com.cloudbrainmed.ai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 挂号表
 */
@Data
@TableName("registration")
public class Registration {
    @TableId
    private String registerId;
    private String patientId;
    private String doctorId;
    private String name;
    private Integer gender;
    private LocalDate birthday;
    private String chiefComplaint;
    private String department;
    private String consultRoom;
    private LocalDate visitDate;
    private LocalTime consultStartTime;
    private LocalTime consultEndTime;
    private BigDecimal price;
    private String payStatus;
    private LocalDateTime createTime;
    private String consultStatus;
}
