package com.cloudbrainmed.patient.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检查/检验申请分组VO - 按挂号分组
 */
public class MedicalOrderGroupVo {
    /**
     * 挂号ID
     */
    private String registerId;

    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 就诊日期
     */
    private String visitDate;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 项目数量
     */
    private Integer itemCount;

    /**
     * 整体状态
     */
    private String status;

    /**
     * 医技申请列表
     */
    private List<MedicalOrderItemVo> items;

    // Getters and Setters
    public String getRegisterId() {
        return registerId;
    }

    public void setRegisterId(String registerId) {
        this.registerId = registerId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(String visitDate) {
        this.visitDate = visitDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<MedicalOrderItemVo> getItems() {
        return items;
    }

    public void setItems(List<MedicalOrderItemVo> items) {
        this.items = items;
    }
}
