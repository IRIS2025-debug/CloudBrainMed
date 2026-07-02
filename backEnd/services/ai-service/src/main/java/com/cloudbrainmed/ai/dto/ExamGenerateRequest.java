package com.cloudbrainmed.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamGenerateRequest {

    @NotBlank(message = "挂号ID不能为空")
    private String registerId;

    @NotNull(message = "上下文信息不能为空")
    private ExamGenerateContext context;

    @Data
    public static class ExamGenerateContext {

        @NotBlank(message = "患者ID不能为空")
        private String patientId;

        private Integer visitAge;

        private String description;
    }
}