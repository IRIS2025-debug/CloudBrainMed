package com.cloudbrainmed.ai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 医生表
 */
@Data
@TableName("doctor")
public class Doctor {
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
    private Integer status;
    private LocalDateTime createTime;
    private Integer isDeleted;
}
