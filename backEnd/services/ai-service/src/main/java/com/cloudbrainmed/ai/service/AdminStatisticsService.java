package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.vo.AdminStatisticsResult;

import java.time.LocalDate;

public interface AdminStatisticsService {
    AdminStatisticsResult patientStatistics(LocalDate start, LocalDate end);

    AdminStatisticsResult registerStatistics(LocalDate start, LocalDate end);

    AdminStatisticsResult medicalStatistics(LocalDate start, LocalDate end);

    AdminStatisticsResult paymentStatistics(LocalDate start, LocalDate end);
}
