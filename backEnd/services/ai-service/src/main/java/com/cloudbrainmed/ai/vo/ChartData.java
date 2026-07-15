package com.cloudbrainmed.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "前端ECharts可直接消费的统一图表数据")
public class ChartData {
    @Schema(description = "图表类型：bar/line/pie/statistic")
    private String type;

    @Schema(description = "图表标题")
    private String title;

    @Schema(description = "X轴分类数据，bar/line使用")
    private List<String> xAxis;

    @Schema(description = "序列数据，bar/line使用")
    private List<SeriesData> series;

    @Schema(description = "饼图或其他扩展数据")
    private Object data;

    @Schema(description = "指标卡数值，statistic使用")
    private Object value;
}
