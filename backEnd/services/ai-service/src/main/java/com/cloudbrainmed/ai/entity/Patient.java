package com.cloudbrainmed.ai.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者表
 */
@Data
public class Patient {

    /**
     * 患者ID
     */
    private String patientId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 性别枚举：1=男，2=女
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private LocalDate birthday;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 密码摘要
     */
    private String password;

    /**
     * 账号状态：0=停用，1=启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 逻辑删除标记：0=未删除，1=已删除
     */
    private Integer isDeleted;
}