package com.cloudbrainmed.common.exception;

import com.cloudbrainmed.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void handlesBusinessExceptionAsBusinessErrorResult() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<?> result = handler.handleBusinessException(new BusinessException("患者不存在"));

        assertThat(result.getCode()).isEqualTo(501);
        assertThat(result.getMsg()).isEqualTo("患者不存在");
        assertThat(result.getData()).isNull();
    }

    @Test
    void handlesRuntimeExceptionAsInternalErrorResult() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<?> result = handler.handleRuntimeException(new RuntimeException("Token 无效，请重新登录"));

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMsg()).isEqualTo("Token 无效，请重新登录");
        assertThat(result.getData()).isNull();
    }
}
