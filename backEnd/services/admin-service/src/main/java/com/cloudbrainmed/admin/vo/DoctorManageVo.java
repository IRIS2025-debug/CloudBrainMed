package com.cloudbrainmed.admin.vo;

import lombok.Data;

@Data
public class DoctorManageVo {
    private String doctorId;
    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private String position;
    private String goodAt;
    private String introduction;
    private String departmentId;
    private String deptName;
    private Integer status;
    private String createTime;
}