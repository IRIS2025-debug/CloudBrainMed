package com.cloudbrainmed.doctor.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 医技申请联表查询结果视图 —— 非表实体，仅用于接收 ExamOrderMapper 的
 * medical_order + patient + medical_order_item + medical_item JOIN 查询结果。
 * 单表实体见 InspectionOrder（映射 medical_order 表）。
 * 统含检查(EXAM)和检验(LAB)两类申请
 */
public class ExamOrder {
    private String orderId;
    private String patientId;
    private String registerId;
    private String doctorId;
    private String patientName;
    private Integer gender;
    private Integer age;
    private String itemCategory;
    private String itemName;
    private BigDecimal price;
    private String payStatus;
    private String urgencyLevel;
    private LocalDateTime createTime;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getRegisterId() { return registerId; }
    public void setRegisterId(String registerId) { this.registerId = registerId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getItemCategory() { return itemCategory; }
    public void setItemCategory(String itemCategory) { this.itemCategory = itemCategory; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getPayStatus() { return payStatus; }
    public void setPayStatus(String payStatus) { this.payStatus = payStatus; }
    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
