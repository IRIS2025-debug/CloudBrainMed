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
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createTime;
    @TableLogic
    private Integer isDeleted;
}
