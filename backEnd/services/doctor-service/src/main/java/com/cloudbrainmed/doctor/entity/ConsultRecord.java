package com.cloudbrainmed.doctor.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 挂号表 registration + 病历表 register_report 联合实体（医生接诊视图）
 */
public class ConsultRecord {
    // registration 字段
    private String registerId;
    private String patientId;
    private String doctorId;
    private String name;
    private Integer gender;
    private LocalDate birthday;
    private String chiefComplaint;
    private String department;
    private String consultRoom;
    private LocalDate visitDate;
    private String consultTime;
    private BigDecimal price;
    private String payStatus;
    private String consultStatus;
    private LocalDateTime createTime;

    // register_report 字段
    private String recordId;
    private String doctorName;
    private String patientName;
    private Integer visitAge;
    private String description;

    // 计算字段
    private Integer patientAge;
    private Integer medicalOrderCount;
    private Integer reportCount;
    private LocalDateTime latestReportTime;
    private Boolean entryAllowed;
    private String entryActionText;
    private String entryBlockedReason;
    private Boolean readOnly;

    public String getRegisterId() { return registerId; }
    public void setRegisterId(String registerId) { this.registerId = registerId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getConsultRoom() { return consultRoom; }
    public void setConsultRoom(String consultRoom) { this.consultRoom = consultRoom; }
    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }
    public String getConsultTime() { return consultTime; }
    public void setConsultTime(String consultTime) { this.consultTime = consultTime; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getPayStatus() { return payStatus; }
    public void setPayStatus(String payStatus) { this.payStatus = payStatus; }
    public String getConsultStatus() { return consultStatus; }
    public void setConsultStatus(String consultStatus) { this.consultStatus = consultStatus; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public Integer getVisitAge() { return visitAge; }
    public void setVisitAge(Integer visitAge) { this.visitAge = visitAge; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPatientAge() { return patientAge; }
    public void setPatientAge(Integer patientAge) { this.patientAge = patientAge; }
    public Integer getMedicalOrderCount() { return medicalOrderCount; }
    public void setMedicalOrderCount(Integer medicalOrderCount) { this.medicalOrderCount = medicalOrderCount; }
    public Integer getReportCount() { return reportCount; }
    public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
    public LocalDateTime getLatestReportTime() { return latestReportTime; }
    public void setLatestReportTime(LocalDateTime latestReportTime) { this.latestReportTime = latestReportTime; }
    public Boolean getEntryAllowed() { return entryAllowed; }
    public void setEntryAllowed(Boolean entryAllowed) { this.entryAllowed = entryAllowed; }
    public String getEntryActionText() { return entryActionText; }
    public void setEntryActionText(String entryActionText) { this.entryActionText = entryActionText; }
    public String getEntryBlockedReason() { return entryBlockedReason; }
    public void setEntryBlockedReason(String entryBlockedReason) { this.entryBlockedReason = entryBlockedReason; }
    public Boolean getReadOnly() { return readOnly; }
    public void setReadOnly(Boolean readOnly) { this.readOnly = readOnly; }

    public Boolean getHasMedicalOrder() {
        return medicalOrderCount != null && medicalOrderCount > 0;
    }

    public Boolean getHasReturnedReport() {
        return reportCount != null && reportCount > 0;
    }
}
