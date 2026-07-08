package com.cloudbrainmed.ai.support;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;

public final class AdminAuthHelper {

    private AdminAuthHelper() {
    }

    public static String requireAdminId(String token, String capabilityName) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("未登录，请先登录");
        }
        try {
            if (!Integer.valueOf(3).equals(DoctorJwtUtil.getRoleType(token))) {
                throw new BusinessException("仅管理员可以使用" + capabilityName);
            }
            return DoctorJwtUtil.getUserId(token);
        } catch (Exception exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException("管理员登录凭证无效");
        }
    }
}
