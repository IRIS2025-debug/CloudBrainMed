package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.mapper.AdminStatisticsMapper;
import com.cloudbrainmed.ai.service.AdminStatisticsService;
import com.cloudbrainmed.ai.service.ChartDataBuilderService;
import com.cloudbrainmed.ai.vo.AdminStatisticsResult;
import com.cloudbrainmed.ai.vo.SummaryItem;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private final AdminStatisticsMapper mapper;
    private final ChartDataBuilderService chartBuilder;

    public AdminStatisticsServiceImpl(AdminStatisticsMapper mapper, ChartDataBuilderService chartBuilder) {
        this.mapper = mapper;
        this.chartBuilder = chartBuilder;
    }

    @Override
    public AdminStatisticsResult patientStatistics(LocalDate start, LocalDate end) {
        AdminStatisticsResult result = new AdminStatisticsResult();
        Long total = mapper.countPatients();
        result.getSummary().add(new SummaryItem("患者总数", total));
        result.getCharts().add(chartBuilder.statistic("患者总数", total));
        result.getCharts().add(chartBuilder.pie("患者性别比例", mapper.countPatientsByGender()));
        result.getCharts().add(chartBuilder.bar("患者年龄分布", "患者数量", mapper.countPatientsByAgeRange()));
        return result;
    }

    @Override
    public AdminStatisticsResult registerStatistics(LocalDate start, LocalDate end) {
        AdminStatisticsResult result = new AdminStatisticsResult();
        Long total = mapper.countRegistrations(start, end);
        result.getSummary().add(new SummaryItem("挂号总数", total));
        result.getCharts().add(chartBuilder.statistic("挂号总数", total));
        result.getCharts().add(chartBuilder.line("每日挂号趋势", "挂号数量", mapper.countDailyRegistrations(start, end)));
        result.getCharts().add(chartBuilder.bar("各科室挂号数量", "挂号数量", mapper.countRegistrationsByDepartment(start, end)));
        result.getCharts().add(chartBuilder.bar("医生接诊排行", "接诊数量", mapper.countRegistrationsByDoctor(start, end)));
        return result;
    }

    @Override
    public AdminStatisticsResult medicalStatistics(LocalDate start, LocalDate end) {
        AdminStatisticsResult result = new AdminStatisticsResult();
        Long orders = mapper.countMedicalOrders(start, end);
        Object completionRate = mapper.medicalItemCompletionRate(start, end);
        Long abnormalReports = mapper.countAbnormalReports(start, end);
        result.getSummary().add(new SummaryItem("检查检验申请数量", orders));
        result.getSummary().add(new SummaryItem("检查检验完成率(%)", completionRate));
        result.getSummary().add(new SummaryItem("异常报告数量", abnormalReports));
        result.getCharts().add(chartBuilder.statistic("检查检验申请数量", orders));
        result.getCharts().add(chartBuilder.statistic("检查检验完成率(%)", completionRate));
        result.getCharts().add(chartBuilder.pie("EXAM/LAB占比", mapper.countMedicalItemsByCategory(start, end)));
        result.getCharts().add(chartBuilder.statistic("异常报告数量", abnormalReports));
        return result;
    }

    @Override
    public AdminStatisticsResult paymentStatistics(LocalDate start, LocalDate end) {
        AdminStatisticsResult result = new AdminStatisticsResult();
        Object total = mapper.sumPaidAmount(start, end);
        result.getSummary().add(new SummaryItem("医院总收入", total));
        result.getCharts().add(chartBuilder.statistic("医院总收入", total));
        result.getCharts().add(chartBuilder.line("每日收入趋势", "收入", mapper.sumDailyPaidAmount(start, end)));
        result.getCharts().add(chartBuilder.pie("支付状态比例", mapper.countPayStatus(start, end)));
        return result;
    }
}
