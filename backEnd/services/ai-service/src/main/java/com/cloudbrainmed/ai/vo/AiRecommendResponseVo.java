package com.cloudbrainmed.ai.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendResponseVo {

    private String parsedDiagnosis;     // AI解析的诊断/症状总结
    private String recommendedDepartment; // 推荐科室
    private String departmentReason;    // 科室推荐理由
    private List<RecommendDoctorVo> doctorRanking; // 医生排行榜
    private String aiAnalysisTime;      // AI分析时间戳
}