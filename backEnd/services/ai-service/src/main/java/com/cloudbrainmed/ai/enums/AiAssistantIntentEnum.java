package com.cloudbrainmed.ai.enums;

/**
 * AI assistant intent for doctor reception.
 *
 * <p>The reception assistant answers reception-context questions, including
 * diagnosis-related assistance. Medical record drafting and prescription review
 * are delegated to their specialized modules.</p>
 */
public enum AiAssistantIntentEnum {
    /** Reception assistance: follow-up, missing information, summary, context Q&A, and diagnosis assistance. */
    RECEPTION_ASSISTANT,

    /** Medical record draft generation, delegated to the medical record module. */
    MEDICAL_RECORD_DRAFT,

    /** Prescription draft generation, delegated to the prescription draft module. */
    PRESCRIPTION_DRAFT,

    /** Prescription or medication risk review, delegated to the prescription review module. */
    PRESCRIPTION_REVIEW,

    /** Unknown or unsupported intent. */
    UNKNOWN;

    public boolean isAssistantHandled() {
        return this == RECEPTION_ASSISTANT;
    }

    public static AiAssistantIntentEnum fromActionType(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return AiAssistantIntentEnum.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }
}
