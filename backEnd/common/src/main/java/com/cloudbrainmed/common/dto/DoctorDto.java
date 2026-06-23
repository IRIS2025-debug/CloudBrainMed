package com.cloudbrainmed.common.dto;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DoctorDto {
    private String doctorId;
    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private String position;
    private String goodAt;
    private String introduction;
    private String departmentId;
    // 医生类型：1看诊医生 2检查医生 3检验医生
    private Integer doctorType;
    private Integer status;
    private LocalDateTime createTime;
}