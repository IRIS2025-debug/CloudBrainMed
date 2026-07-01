package com.cloudbrainmed.common.exception;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.result.ResultCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.error(ResultCode.BUSINESS_ERROR, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        return Result.error(ResultCode.INTERNAL_ERROR, e.getMessage());
    }
}
