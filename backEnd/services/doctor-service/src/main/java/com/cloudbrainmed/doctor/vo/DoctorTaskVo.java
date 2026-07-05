package com.cloudbrainmed.doctor.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 医生任务列表 VO
 * 返回给前端的任务列表数据
 */
@Data
public class DoctorTaskVo {
    private String orderItemId;
    private String orderId;
    private String itemCode;
    private String itemName;
    private String itemCategory;
    private String urgencyLevel;
    private BigDecimal price;
    private String status;
    private String createTime;
    private String assignTime;
    private String patientId;
    private String registerId;
    private String patientName;
    private Integer gender;
    private Integer age;
    private String clinicalSummary;
    /** 等待分钟数（从 createTime 到现在的时长） */
    private Long waitingMinutes;
    /** 是否已触发老化升级（NORMAL > 30min → URGENT） */
    private Boolean agingPromoted;
    /** 状态标签（由服务端计算） */
    private String statusLabel;
    /** 紧急程度标签（由服务端计算，包含老化提升标识） */
    private String urgencyLabel;

    /** 计算等待分钟数（从 createTime 到现在的时长） */
    public void calculateWaitingMinutes() {
        if (this.createTime != null) {
            LocalDateTime ct = LocalDateTime.parse(this.createTime);
            this.waitingMinutes = Duration.between(ct, LocalDateTime.now()).toMinutes();
        }
    }
}