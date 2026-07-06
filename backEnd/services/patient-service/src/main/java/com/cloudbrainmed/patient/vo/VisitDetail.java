package com.cloudbrainmed.patient.vo;

import com.cloudbrainmed.patient.entity.*;
import lombok.Data;
import java.util.List;

@Data
public class VisitDetail {
    private Registration register;
    private RegisterReport report;
    private List<MedicalOrder> orders;
    private List<MedicalOrderItem> orderItems;  // 单独存储明细
    private List<Prescription> prescriptions;
}