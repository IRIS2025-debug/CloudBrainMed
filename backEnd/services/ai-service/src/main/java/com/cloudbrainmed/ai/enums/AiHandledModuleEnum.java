package com.cloudbrainmed.ai.enums;

public enum AiHandledModuleEnum {
    MEDICAL_RECORD("AI_MEDICAL_RECORD"),
    PRESCRIPTION_DRAFT("AI_PRESCRIPTION_DRAFT"),
    PRESCRIPTION_REVIEW("AI_PRESCRIPTION_REVIEW");

    private final String code;

    AiHandledModuleEnum(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
