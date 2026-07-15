package com.cloudbrainmed.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "管理员运营数据智能分析请求")
public class AdminAnalysisRequest {

    @NotBlank(message = "分析问题不能为空")
    @Schema(description = "管理员自然语言问题", example = "分析最近30天医院运行情况")
    private String question;

    @Schema(description = "统计开始日期", example = "2026-06-01")
    private LocalDate startTime;

    @Schema(description = "统计结束日期", example = "2026-06-30")
    private LocalDate endTime;
}
