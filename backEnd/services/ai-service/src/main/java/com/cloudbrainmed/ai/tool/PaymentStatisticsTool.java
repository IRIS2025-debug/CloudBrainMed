package com.cloudbrainmed.ai.tool;

import com.cloudbrainmed.ai.service.AdminStatisticsService;
import com.cloudbrainmed.ai.vo.AdminStatisticsResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PaymentStatisticsTool {
    private final AdminStatisticsService statisticsService;

    public PaymentStatisticsTool(AdminStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    public AdminStatisticsResult execute(LocalDate start, LocalDate end) {
        return statisticsService.paymentStatistics(start, end);
    }
}
