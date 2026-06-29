package com.cloudbrainmed.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SchedulePlanItemUpdateRequest {
    @NotBlank
    private String doctorId;
    @NotNull
    private LocalDate workDate;
    @NotNull
    private LocalTime startTime;
    @NotNull
    private LocalTime endTime;
    @Min(1)
    private Integer maxNum;
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal price;
    @NotBlank
    private String room;
}
