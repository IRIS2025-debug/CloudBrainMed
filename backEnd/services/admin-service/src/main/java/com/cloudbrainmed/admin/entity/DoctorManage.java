package com.cloudbrainmed.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("doctor")
public class DoctorManage {
    @TableId
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
    private Date createTime;
    private Integer isDeleted;
}