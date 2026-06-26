package com.cloudbrainmed.doctor.entity;

import java.time.LocalDateTime;

/**
 * 医技申请主表实体（映射 medical_order 表）
 * 统含检查(EXAM)和检验(LAB)两类申请
 */
public class MedicalOrder {
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
