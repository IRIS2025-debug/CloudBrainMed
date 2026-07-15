package com.cloudbrainmed.ai.tool;

import com.cloudbrainmed.ai.service.AdminStatisticsService;
import com.cloudbrainmed.ai.vo.AdminStatisticsResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class MedicalStatisticsTool {
    private final AdminStatisticsService statisticsService;

    public MedicalStatisticsTool(AdminStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    public AdminStatisticsResult execute(LocalDate start, LocalDate end) {
        return statisticsService.medicalStatistics(start, end);
    }
}
