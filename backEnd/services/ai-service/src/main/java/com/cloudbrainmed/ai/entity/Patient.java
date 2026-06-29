package com.cloudbrainmed.ai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者表
 */
@Data
@TableName("patient")
public class Patient {
    @TableId
    private String patientId;
    private String name;
    private Integer gender;
    private LocalDate birthday;
    private String phone;
    private String idCard;
    private String address;
    private String password;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
