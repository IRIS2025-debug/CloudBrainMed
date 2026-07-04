package com.cloudbrainmed.doctor.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MedicalReport {
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
}
