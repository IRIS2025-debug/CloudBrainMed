package com.cloudbrainmed.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "管理员运营数据智能分析响应")
public class AdminAnalysisResponse {
    @Schema(description = "AI生成的运营分析文本")
    private String analysis;

    @Schema(description = "关键指标摘要")
    private List<SummaryItem> summary;

    @Schema(description = "前端ECharts图表数据")
    private List<ChartData> charts;
}
