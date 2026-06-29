package com.cloudbrainmed.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class AiScheduleTimeWindow {

    /**
     * 1-7 means Monday-Sunday. Null means every day in the requested period.
     */
    @Min(1)
    @Max(7)
    private Integer dayOfWeek;

    private LocalTime startTime;
    private LocalTime endTime;

    @Min(1)
    @Max(500)
    private Integer maxNum;

    private BigDecimal price;

    @Size(max = 64)
    private String room;
}
