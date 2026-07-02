package com.cloudbrainmed.doctor.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MedicalOrderItem {
    private String orderItemId;
    private String orderId;
    private String itemId;
    private String itemCode;
    private String itemName;
    private String itemCategory;
    private String assignedDeptId;
    private String assignedDoctorId;
    private String urgencyLevel;
    private BigDecimal price;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime assignTime;
    private LocalDateTime completeTime;
}