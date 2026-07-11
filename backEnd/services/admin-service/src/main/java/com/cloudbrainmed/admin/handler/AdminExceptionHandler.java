package com.cloudbrainmed.admin.handler;

import com.cloudbrainmed.admin.exception.AdminAuthException;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 优先级高于兜底的 GlobalExceptionHandler，保证 admin-service 内
// BusinessException / 鉴权异常的返回码稳定，不会漂移到 500。
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AdminExceptionHandler {

    /**
     * 管理员资料接口鉴权失败：按异常携带的语义返回 401（未认证）或 403（无权限）。
     */
    @ExceptionHandler(AdminAuthException.class)
    public Result<?> handleAuth(AdminAuthException exception) {
        return Result.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({
        BusinessException.class,
        IllegalArgumentException.class
    })
    public Result<?> handleBusiness(RuntimeException exception) {
        return Result.error(400, exception.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class
    })
    public Result<?> handleValidation(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException method) {
            return Result.error(400, method.getBindingResult()
                    .getAllErrors().get(0).getDefaultMessage());
        }
        BindException bind = (BindException) exception;
        return Result.error(400, bind.getBindingResult()
                .getAllErrors().get(0).getDefaultMessage());
    }
}
