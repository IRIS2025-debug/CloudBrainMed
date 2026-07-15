package com.cloudbrainmed.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "关键指标摘要")
public class SummaryItem {
    @Schema(description = "指标标题")
    private String title;

    @Schema(description = "指标数值")
    private Object value;
}
