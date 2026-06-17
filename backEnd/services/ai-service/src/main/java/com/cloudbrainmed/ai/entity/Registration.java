package com.cloudbrainmed.ai.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 挂号表
 */
@Data
public class Registration {

    /**
     * 挂号ID
     */
    private String registerId;

    /**
     * 患者ID
     */
    private String patientId;

    /**
     * 医生ID
     */
    private String doctorId;

    /**
     * 名称或姓名
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
     * 患者主诉
     */
    private String chiefComplaint;

    /**
     * 科室名称快照
     */
    private String department;

    /**
     * 接诊房间
     */
    private String consultRoom;

    /**
     * 就诊日期
     */
    private LocalDate visitDate;

    /**
     * 接诊开始时间，格式为 HH:mm:ss
     */
    private LocalTime consultStartTime;

    /**
     * 接诊结束时间，格式为 HH:mm:ss，必须晚于 consult_start_time
     */
    private LocalTime consultEndTime;

    /**
     * 金额
     */
    private BigDecimal price;

    /**
     * 支付状态：WAITING=待支付，PAID=已支付，CANCELLED=已取消，REFUNDED=已退款
     */
    private String payStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 接诊状态：PENDING=待接诊，IN_PROGRESS=接诊中，RECORD_CONFIRMED=病历已确认，COMPLETED=已完成
     */
    private String consultStatus;
}