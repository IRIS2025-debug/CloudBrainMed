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
    private String assignedDoctorName;
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
    /** 状态标签（由服务端计算） */
    private String statusLabel;
    /** 紧急程度标签（由服务端计算，包含老化提升标识） */
    private String urgencyLabel;

    public String getGenderLabel() {
        return gender == 1 ? "男" : gender == 0 ? "女" : "未知";
    }

    public String getItemCategoryLabel() {
        return "EXAM".equals(itemCategory) ? "检查" : "LAB".equals(itemCategory) ? "检验" : itemCategory;
    }
}