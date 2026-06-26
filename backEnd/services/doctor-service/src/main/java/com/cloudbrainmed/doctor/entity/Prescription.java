package com.cloudbrainmed.doctor.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 处方实体（映射 prescription 表）
 */
@Data
@TableName("prescription")
public class Prescription {
    @TableId
    private String prescriptionId;
    private String registerId;
    private String patientId;
    private String doctorId;
    private String medicineId;
    private String patientName;
    private String doctorName;
    private String medicineName;
    private String spec;
    private String usage;
    private Integer num;
    private LocalDate prescriptionDate;
    private BigDecimal price;
    private String payStatus;
    private LocalDateTime createTime;
}
