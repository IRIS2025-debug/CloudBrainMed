package com.cloudbrainmed.doctor.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 医生任务详情 VO
 */
@Data
public class DoctorTaskDetailVo {
    private String orderItemId;
    private String orderId;
    private String itemCode;
    private String itemName;
    private String itemCategory;
    private String urgencyLevel;
    private BigDecimal price;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime assignTime;
    private LocalDateTime completeTime;
    private String patientId;
    private String registerId;
    private String requesterDoctorId;
    private String patientName;
    private Integer gender;
    private LocalDate birthday;
    private Integer age;
    private String clinicalSummary;

    public String getGenderLabel() {
        return gender == 1 ? "男" : gender == 0 ? "女" : "未知";
    }

    public String getStatusLabel() {
        return switch (status) {
            case "WAITING_ASSIGN" -> "待分配";
            case "QUEUED" -> "排队中";
            case "IN_PROCESS" -> "处理中";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    public String getUrgencyLabel() {
        return switch (urgencyLevel) {
            case "EMERGENCY" -> "紧急";
            case "URGENT" -> "加急";
            case "NORMAL" -> "常规";
            default -> urgencyLevel;
        };
    }

    public String getItemCategoryLabel() {
        return "EXAM".equals(itemCategory) ? "检查" : "LAB".equals(itemCategory) ? "检验" : itemCategory;
    }
}