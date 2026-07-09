package com.cloudbrainmed.ai.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI科室推荐结果DTO（结构化输出）
 * 用于从AI获取科室推荐、症状总结和推荐理由
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDepartmentRecommendationDto {
    @JsonProperty("parsed_diagnosis")
    private String parsedDiagnosis;

    @JsonProperty("recommended_departments")
    private List<String> recommendedDepartments;

    @JsonProperty("department_reason")
    private String departmentReason;

    @JsonProperty("emergency")
    private Boolean emergency;
}