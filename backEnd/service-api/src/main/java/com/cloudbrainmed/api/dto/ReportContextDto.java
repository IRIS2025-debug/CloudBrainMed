package com.cloudbrainmed.api.dto;

import java.util.ArrayList;
import java.util.List;

public class ReportContextDto {
    private boolean available = true;
    private String errorMessage;
    private String registerId;
    private String patientId;
    private Integer patientAge;
    private String patientGender;
    private String chiefComplaint;
    private String currentRecordDesc;
    private List<String> medicalHistory = new ArrayList<>();
    private List<String> previousReports = new ArrayList<>();

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRegisterId() {
        return registerId;
    }

    public void setRegisterId(String registerId) {
        this.registerId = registerId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Integer getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(Integer patientAge) {
        this.patientAge = patientAge;
    }

    public String getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(String patientGender) {
        this.patientGender = patientGender;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public String getCurrentRecordDesc() {
        return currentRecordDesc;
    }

    public void setCurrentRecordDesc(String currentRecordDesc) {
        this.currentRecordDesc = currentRecordDesc;
    }

    public List<String> getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(List<String> medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public List<String> getPreviousReports() {
        return previousReports;
    }

    public void setPreviousReports(List<String> previousReports) {
        this.previousReports = previousReports;
    }
}
