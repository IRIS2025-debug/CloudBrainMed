package com.cloudbrainmed.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginDto {

    private String phone;
    private String password;
    private Integer roleType; // 1患者 2医生 3管理员
    private Integer doctorType; // 医生子类型：1接诊 2检查 3检验（roleType=2 时使用）
}