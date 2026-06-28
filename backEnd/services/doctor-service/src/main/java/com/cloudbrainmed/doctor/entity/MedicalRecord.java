package com.cloudbrainmed.doctor.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 病历实体（映射 medical_record 表）
 */
public class MedicalRecord {
    private String recordId;
    private String patientId;
    private String doctorId;
    private String registerId;
    private String doctorName;
    private String patientName;
    private Integer visitAge;
    private String description;
    private LocalDate visitDate;
    private String payStatus;
    private LocalDateTime createTime;

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getRegisterId() { return registerId; }
    public void setRegisterId(String registerId) { this.registerId = registerId; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public Integer getVisitAge() { return visitAge; }
    public void setVisitAge(Integer visitAge) { this.visitAge = visitAge; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }
    public String getPayStatus() { return payStatus; }
    public void setPayStatus(String payStatus) { this.payStatus = payStatus; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
