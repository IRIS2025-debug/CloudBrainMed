package com.cloudbrainmed.admin.exception;

/**
 * 管理员资料接口鉴权异常。
 * 携带明确的 HTTP 语义状态码，由 {@code AdminExceptionHandler} 稳定映射：
 * <ul>
 *   <li>401 未认证：token 缺失、非法、篡改、过期、伪造或缺少 userId</li>
 *   <li>403 无权限：token 合法但角色不是管理员（roleType != 3）</li>
 * </ul>
 * 仅用于 admin-service 自身的管理员身份校验，不影响其他模块的业务异常语义。
 */
public class AdminAuthException extends RuntimeException {

    private final int code;

    private AdminAuthException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /** 401：未认证（无凭证或凭证非法）。 */
    public static AdminAuthException unauthorized(String message) {
        return new AdminAuthException(401, message);
    }

    /** 403：已认证但无管理员权限。 */
    public static AdminAuthException forbidden(String message) {
        return new AdminAuthException(403, message);
    }
}
