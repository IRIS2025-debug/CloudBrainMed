package com.cloudbrainmed.patient.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.OffsetDateTime ;

@Data
@TableName("doctor")
public class Doctor {
    @TableId(type = IdType.ASSIGN_ID)
    private String doctorId;
    private String avatar;
    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private String position;
    private String goodAt;
    private String introduction;
    private String password;
    private String departmentId;
    // 医生类型：1看诊医生 2检查医生 3检验医生
    private Integer doctorType;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createTime;
    @TableLogic
    private Integer isDeleted;
}