package com.cloudbrainmed.ai.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI科室推荐结果DTO（结构化输出）
 * 用于从AI获取科室推荐、症状总结和推荐理由
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDepartmentRecommendationDto {

    /**
     * 症状总结（20字以内）
     */
    @JSONField(name = "parsed_diagnosis")
    private String parsedDiagnosis;

    /**
     * 推荐的科室名称（必须从系统科室列表中选择）
     */
    @JSONField(name = "recommended_department")
    private String recommendedDepartment;

    /**
     * 推荐理由（30字以内）
     */
    @JSONField(name = "department_reason")
    private String departmentReason;

    /**
     * 是否紧急（true表示需要立即就医）
     */
    @JSONField(name = "emergency")
    private Boolean emergency;
}