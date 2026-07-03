package com.cloudbrainmed.patient.vo;

import com.cloudbrainmed.patient.entity.Prescription;
import java.math.BigDecimal;
import java.util.List;

public class RegisterPrescriptionGroupVo {
    private String registerId;
    private String patientName;
    private String doctorName;
    private String prescriptionDate;
    private BigDecimal totalAmount;
    private Integer prescriptionCount;
    private String payStatus;
    private List<Prescription> prescriptions;

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

    public String getPrescriptionDate() {
        return prescriptionDate;
    }

    public void setPrescriptionDate(String prescriptionDate) {
        this.prescriptionDate = prescriptionDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getPrescriptionCount() {
        return prescriptionCount;
    }

    public void setPrescriptionCount(Integer prescriptionCount) {
        this.prescriptionCount = prescriptionCount;
    }

    public String getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }

    public List<Prescription> getPrescriptions() {
        return prescriptions;
    }

    public void setPrescriptions(List<Prescription> prescriptions) {
        this.prescriptions = prescriptions;
    }
}
