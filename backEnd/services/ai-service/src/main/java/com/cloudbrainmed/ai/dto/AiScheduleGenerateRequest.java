package com.cloudbrainmed.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiScheduleGenerateRequest {

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
    private LocalDate periodStart;

    @NotNull
    private LocalDate periodEnd;

    @Size(max = 1000)
    private String requirement;

    @Min(1)
    @Max(500)
    private Integer defaultMaxNum = 30;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal defaultPrice = BigDecimal.ZERO;

    @Size(max = 20)
    private List<@Size(max = 64) String> rooms = new ArrayList<>();

    @Valid
    @Size(max = 30)
    private List<AiScheduleTimeWindow> timeWindows =
            new ArrayList<>();

    @Size(max = 60)
    private List<LocalDate> unavailableDates = new ArrayList<>();

    @AssertTrue(message = "periodStart must not be after periodEnd")
    public boolean isValidPeriodOrder() {
        return periodStart == null
                || periodEnd == null
                || !periodStart.isAfter(periodEnd);
    }

    @AssertTrue(message = "AI schedule period must be within 31 days")
    public boolean isValidPeriodLength() {
        return periodStart == null
                || periodEnd == null
                || ChronoUnit.DAYS.between(periodStart, periodEnd) <= 30;
    }
}
