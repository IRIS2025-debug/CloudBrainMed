package com.cloudbrainmed.ai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DateValueStat {
    private LocalDate statDate;
    private BigDecimal value;
}
