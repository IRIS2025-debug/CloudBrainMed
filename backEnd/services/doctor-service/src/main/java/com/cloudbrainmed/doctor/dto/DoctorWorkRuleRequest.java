package com.cloudbrainmed.doctor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class DoctorWorkRuleRequest {

    @NotNull
    @Min(1)
    @Max(7)
    private Integer dayOfWeek;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @NotNull
    @Min(1)
    private Integer maxPatients;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer preferredLevel;

    @NotNull
    private LocalDate validFrom;

    @NotNull
    private LocalDate validTo;
}
