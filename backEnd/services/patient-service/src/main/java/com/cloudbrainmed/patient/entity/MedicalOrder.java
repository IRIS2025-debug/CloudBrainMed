// MedicalOrder.java
package com.cloudbrainmed.patient.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 医技申请主表 medical_order
 * 用途：保存一次脑科检查或检验申请的患者、挂号、申请医生、临床摘要和整体流程状态
 */
@TableName("medical_order")  // 添加这个注解
public class MedicalOrder {

    // ===== 字段 =====
    @TableId(type = IdType.ASSIGN_ID)  // 添加这个注解，标记主键
    private String orderId;
    private String patientId;
    private String registerId;
    private String doctorId;
    private String clinicalSummary;
    private String urgencyLevel;
    private String sourceType;
    private String aiTraceId;
    private String status;
    private String payStatus;
    private LocalDateTime confirmedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ===== 常量定义 =====
    public static final String URGENCY_NORMAL = "NORMAL";
    public static final String URGENCY_URGENT = "URGENT";
    public static final String URGENCY_EMERGENCY = "EMERGENCY";

    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_AI_ASSISTED = "AI_ASSISTED";

    public static final String STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    public static final String STATUS_WAITING_ASSIGN = "WAITING_ASSIGN";
    public static final String STATUS_QUEUED = "QUEUED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String PAY_WAITING = "WAITING";
    public static final String PAY_PAID = "PAID";
    public static final String PAY_CANCELLED = "CANCELLED";
    public static final String PAY_REFUNDED = "REFUNDED";

    // ===== Getters and Setters =====
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getRegisterId() { return registerId; }
    public void setRegisterId(String registerId) { this.registerId = registerId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getClinicalSummary() { return clinicalSummary; }
    public void setClinicalSummary(String clinicalSummary) { this.clinicalSummary = clinicalSummary; }
    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getAiTraceId() { return aiTraceId; }
    public void setAiTraceId(String aiTraceId) { this.aiTraceId = aiTraceId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPayStatus() { return payStatus; }
    public void setPayStatus(String payStatus) { this.payStatus = payStatus; }
    public LocalDateTime getConfirmedTime() { return confirmedTime; }
    public void setConfirmedTime(LocalDateTime confirmedTime) { this.confirmedTime = confirmedTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}