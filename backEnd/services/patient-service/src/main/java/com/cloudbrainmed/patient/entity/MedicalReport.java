// src/main/java/com/cloudbrainmed/patient/entity/MedicalReport.java
package com.cloudbrainmed.patient.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("medical_report")
public class MedicalReport {
    @TableId(type = IdType.INPUT)
    private String reportId;
    private String orderItemId;
    private String patientId;
    private String itemCategory;
    private String resultSummary;
    private String conclusion;
    private String abnormalFlag;
    private String attachmentUrl;
    private String reportDoctorId;
    private String status;
    private LocalDateTime performedTime;
    private LocalDateTime reportTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String followUpAdvice;

    // Getters and Setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getOrderItemId() { return orderItemId; }
    public void setOrderItemId(String orderItemId) { this.orderItemId = orderItemId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getItemCategory() { return itemCategory; }
    public void setItemCategory(String itemCategory) { this.itemCategory = itemCategory; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public String getAbnormalFlag() { return abnormalFlag; }
    public void setAbnormalFlag(String abnormalFlag) { this.abnormalFlag = abnormalFlag; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    public String getReportDoctorId() { return reportDoctorId; }
    public void setReportDoctorId(String reportDoctorId) { this.reportDoctorId = reportDoctorId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getPerformedTime() { return performedTime; }
    public void setPerformedTime(LocalDateTime performedTime) { this.performedTime = performedTime; }
    public LocalDateTime getReportTime() { return reportTime; }
    public void setReportTime(LocalDateTime reportTime) { this.reportTime = reportTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getFollowUpAdvice() { return followUpAdvice; }
    public void setFollowUpAdvice(String followUpAdvice) { this.followUpAdvice = followUpAdvice; }
}