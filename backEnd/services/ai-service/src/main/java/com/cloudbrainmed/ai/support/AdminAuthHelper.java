package com.cloudbrainmed.ai.support;

import com.cloudbrainmed.common.utils.DoctorJwtUtil;

public final class AdminAuthHelper {

    private AdminAuthHelper() {
    }

    public static String requireAdminId(String token, String capabilityName) {
        if (token == null || token.isBlank()) {
            throw AdminAuthException.unauthorized("未登录，请先登录");
        }
        try {
            if (!Integer.valueOf(3).equals(
                    DoctorJwtUtil.getRoleType(token))) {
                throw AdminAuthException.forbidden(
                        "仅管理员可以使用" + capabilityName);
            }
            String adminId = DoctorJwtUtil.getUserId(token);
            if (adminId == null || adminId.isBlank()) {
                throw AdminAuthException.unauthorized("管理员登录凭证无效");
            }
            return adminId;
        } catch (Exception exception) {
            if (exception instanceof AdminAuthException authException) {
                throw authException;
            }
            throw AdminAuthException.unauthorized("管理员登录凭证无效");
        }
    }
}
