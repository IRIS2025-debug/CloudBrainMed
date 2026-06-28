package com.cloudbrainmed.admin.handler;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AdminExceptionHandler {

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
