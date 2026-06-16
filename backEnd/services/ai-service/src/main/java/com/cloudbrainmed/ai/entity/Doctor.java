package com.cloudbrainmed.ai.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 医生表
 */
@Data
public class Doctor {

    /**
     * 医生ID
     */
    private String doctorId;

    /**
     * 头像访问地址
     */
    private String avatar;

    /**
     * 名称或姓名
     */
    private String name;

    /**
     * 性别枚举：1=男，2=女
     */
    private Integer gender;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 职称或职位
     */
    private String position;

    /**
     * 擅长领域
     */
    private String goodAt;

    /**
     * 个人简介
     */
    private String introduction;

    /**
     * 密码摘要
     */
    private String password;

    /**
     * 所属科室ID
     */
    private String departmentId;

    /**
     * 账号状态：0=停用，1=启用（正常）
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