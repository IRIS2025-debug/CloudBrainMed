package com.cloudbrainmed.doctor.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MedicalReportVo {
    private String reportId;
    private String orderItemId;
    private String orderId;
    private String registerId;
    private String patientId;
    private String itemCode;
    private String itemName;
    private String itemCategory;
    private String resultSummary;
    private String conclusion;
    private String abnormalFlag;
    private String attachmentUrl;
    private String reportDoctorId;
    private String status;
    private LocalDateTime performedTime;
    private LocalDateTime reportTime;
}
