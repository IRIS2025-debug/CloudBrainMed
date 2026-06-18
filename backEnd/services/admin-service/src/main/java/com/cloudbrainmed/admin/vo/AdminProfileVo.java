package com.cloudbrainmed.admin.vo;

import lombok.Data;

@Data
public class AdminProfileVo {
    private String adminId;
    private String avatar;
    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private String position;
}