package com.cloudbrainmed.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("admin")
public class Account {
    @TableId
    private String adminId;
    private String avatar;
    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private String position;
    private String password;
    private Integer status;
    private Date createTime;
    private Integer isDeleted;
}