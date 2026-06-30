package com.cloudbrainmed.ai.dto;

import lombok.Data;

@Data
public class MedicineQueryDto {
    private String sessionId;
    private String question;
    private String medicineId;
    /** 用户角色：doctor（医生）或 patient（患者），默认 doctor */
    private String userRole;
    /** 患者ID（患者端调用时携带） */
    private String patientId;
}