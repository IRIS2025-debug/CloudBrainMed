package com.cloudbrainmed.patient.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@TableName("registration")
public class Registration {
    @TableId(type = IdType.ASSIGN_ID)
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
    private String consultTime;
    private BigDecimal price;
    private String payStatus;
    private String consultStatus;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createTime;
}
