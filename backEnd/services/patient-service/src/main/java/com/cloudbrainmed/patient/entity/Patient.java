package com.cloudbrainmed.patient.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("patient")
public class Patient {
    @TableId(type = IdType.ASSIGN_ID)
    private String patientId;
    private String name;
    private Integer gender;
    private String phone;
    private String idCard;
    private String avatar;
    private String address;
    private String password;
    private LocalDate birthday;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}
