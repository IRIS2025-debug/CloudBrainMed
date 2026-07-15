package com.cloudbrainmed.ai.tool;

import com.cloudbrainmed.ai.service.AdminStatisticsService;
import com.cloudbrainmed.ai.vo.AdminStatisticsResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PatientStatisticsTool {
    private final AdminStatisticsService statisticsService;

    public PatientStatisticsTool(AdminStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    public AdminStatisticsResult execute(LocalDate start, LocalDate end) {
        return statisticsService.patientStatistics(start, end);
    }
}
