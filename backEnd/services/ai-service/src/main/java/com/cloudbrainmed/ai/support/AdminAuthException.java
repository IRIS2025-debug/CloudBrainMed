package com.cloudbrainmed.ai.support;

public class AdminAuthException extends IllegalArgumentException {

    private final int code;

    private AdminAuthException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static AdminAuthException unauthorized(String message) {
        return new AdminAuthException(401, message);
    }

    public static AdminAuthException forbidden(String message) {
        return new AdminAuthException(403, message);
    }
}
