package com.cloudbrainmed.auth.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Doctor {
    // 医生ID
    private String doctorId;
    // 头像
    private String avatar;
    // 姓名
    private String name;
    // 性别
    private Integer gender;
    // 电话
    private String phone;
    // 邮箱
    private String email;
    // 职位
    private String position;
    // 擅长
    private String goodAt;
    // 简介
    private String introduction;
    // 密码
    private String password;
    // 科室ID
    private String departmentId;
    // 医生类型：1看诊医生 2检查医生 3检验医生
    private Integer doctorType;
    // 0停用 1正常
    private Integer status;
    // 创建时间
    private Date createTime;
    // 0未删除 1已删除
    private Integer isDeleted;
}