package com.cloudbrainmed.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class AiScheduleDto {

    @NotNull
    private LocalDate periodStart;

    @NotNull
    private LocalDate periodEnd;

    @NotBlank
    private String deptId;

    private String strategy = "WORKLOAD_BALANCED";

    @Min(15)
    @Max(240)
    private Integer slotMinutes = 60;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal defaultPrice;

    @NotEmpty
    private List<@NotBlank String> rooms;
}
