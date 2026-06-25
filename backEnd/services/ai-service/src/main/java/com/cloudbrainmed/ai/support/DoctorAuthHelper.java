package com.cloudbrainmed.ai.support;

import com.cloudbrainmed.common.utils.DoctorJwtUtil;

public final class DoctorAuthHelper {

    private DoctorAuthHelper() {
    }

    public static String requireDoctorId(String token, String capabilityName) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("未登录，请先登录");
        }
        try {
            if (!Integer.valueOf(2).equals(
                    DoctorJwtUtil.getRoleType(token))) {
                throw new IllegalArgumentException(
                        "仅医生可以使用" + capabilityName);
            }
            return DoctorJwtUtil.getUserId(token);
        } catch (Exception exception) {
            if (exception instanceof IllegalArgumentException argumentException) {
                throw argumentException;
            }
            throw new IllegalArgumentException("医生登录凭证无效");
        }
    }
}
