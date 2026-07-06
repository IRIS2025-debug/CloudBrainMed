package com.cloudbrainmed.patient.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 病历报告实体类
 * 对应表: register_report
 */
@Data
@TableName("register_report")
public class RegisterReport {

    /**
     * 病历记录ID - 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String recordId;

    /**
     * 患者ID
     */
    private String patientId;

    /**
     * 医生ID
     */
    private String doctorId;

    /**
     * 挂号ID
     */
    private String registerId;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 就诊年龄
     */
    private Integer visitAge;

    /**
     * 病历描述/主诉详情
     */
    private String description;

    /**
     * 就诊日期
     */
    private LocalDate visitDate;

    /**
     * 支付状态: WAITING-待支付, PAID-已支付, REFUNDED-已退款
     */
    private String payStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}