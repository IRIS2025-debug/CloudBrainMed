package com.cloudbrainmed.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class MedicineChatRequest {

    @NotBlank(message = "patientId不能为空")
    private String patientId;

    @NotBlank(message = "message不能为空")
    private String message;

    private String sessionId;

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
