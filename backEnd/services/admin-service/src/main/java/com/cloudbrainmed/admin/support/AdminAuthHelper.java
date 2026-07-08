package com.cloudbrainmed.admin.support;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthHelper {

    public String requireAdmin(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("未登录，请先登录");
        }
        try {
            Integer roleType = DoctorJwtUtil.getRoleType(token);
            if (!Integer.valueOf(3).equals(roleType)) {
                throw new BusinessException("仅管理员可访问");
            }
            return DoctorJwtUtil.getUserId(token);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("管理员登录凭证无效");
        }
    }
}
