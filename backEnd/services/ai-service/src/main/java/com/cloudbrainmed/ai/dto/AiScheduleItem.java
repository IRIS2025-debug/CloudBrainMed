package com.cloudbrainmed.ai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AiScheduleItem {

    @NotBlank
    @Size(max = 32)
    private String doctorId;

    @NotBlank
    @Size(max = 64)
    private String doctorName;

    @NotBlank
    @Size(max = 32)
    private String deptId;

    @NotNull
    private LocalDate workDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @NotNull
    @Min(1)
    @Max(500)
    private Integer maxNum;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal price;

    @Size(max = 64)
    private String room;

    private boolean conflict;

    @Size(max = 64)
    private String conflictType;

    @Size(max = 200)
    private String conflictReason;
}
