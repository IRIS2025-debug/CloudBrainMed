package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.AdminAnalysisRequest;
import com.cloudbrainmed.ai.service.AdminDataAnalysisAgent;
import com.cloudbrainmed.ai.vo.AdminAnalysisResponse;
import com.cloudbrainmed.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping({"/ai/admin", "/ai-service/ai/admin"})
@RequiredArgsConstructor
@Tag(name = "管理员医疗运营数据统计分析智能体")
public class AdminAgentController {

    private final AdminDataAnalysisAgent adminDataAnalysisAgent;

    @PostMapping("/analyze")
    @Operation(summary = "管理员自然语言运营数据统计分析")
    public Result<AdminAnalysisResponse> analyze(@Valid @RequestBody AdminAnalysisRequest request) {
        return Result.success(adminDataAnalysisAgent.analyze(request));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public Result<Void> handleBadRequest(RuntimeException exception) {
        return Result.error(400, exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidationException(Exception exception) {
        String message = "请求参数不合法";
        if (exception instanceof MethodArgumentNotValidException validException
                && validException.getBindingResult().getFieldError() != null) {
            message = validException.getBindingResult().getFieldError().getDefaultMessage();
        } else if (exception instanceof BindException bindException
                && bindException.getBindingResult().getFieldError() != null) {
            message = bindException.getBindingResult().getFieldError().getDefaultMessage();
        }
        return Result.error(400, message);
    }

    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleSqlException(DataAccessException exception) {
        log.error("管理员运营统计SQL查询失败", exception);
        return Result.error(500, "统计数据查询失败，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("管理员运营数据智能分析失败", exception);
        return Result.error("运营数据智能分析失败，请稍后重试");
    }
}
