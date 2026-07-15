package com.cloudbrainmed.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ECharts序列数据")
public class SeriesData {
    @Schema(description = "序列名称")
    private String name;

    @Schema(description = "序列数据")
    private List<Object> data;
}
