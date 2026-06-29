package com.cloudbrainmed.common.constant;

import java.util.Locale;

public enum UrgencyLevelEnum {
    NORMAL,
    URGENT,
    EMERGENCY;

    public static UrgencyLevelEnum from(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "urgencyLevel必须为NORMAL、URGENT或EMERGENCY");
        }
    }
}
