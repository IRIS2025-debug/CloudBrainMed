package com.cloudbrainmed.ai.enums;

public enum AiCallSourceEnum {
    ASSISTANT_CHAT("AI_ASSISTANT_CHAT"),
    MEDICAL_RECORD_GENERATE("AI_MEDICAL_RECORD_GENERATE"),
    PRESCRIPTION_REVIEW("AI_PRESCRIPTION_REVIEW"),
    SCHEDULE_GENERATE("AI_SCHEDULE_GENERATE"),
    SCHEDULE_PUBLISH("AI_SCHEDULE_PUBLISH");

    private final String code;

    AiCallSourceEnum(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
