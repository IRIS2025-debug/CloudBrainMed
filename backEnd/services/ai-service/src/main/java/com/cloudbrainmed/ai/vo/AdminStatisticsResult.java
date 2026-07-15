package com.cloudbrainmed.ai.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminStatisticsResult {
    private List<SummaryItem> summary = new ArrayList<>();
    private List<ChartData> charts = new ArrayList<>();

    public void merge(AdminStatisticsResult other) {
        if (other == null) {
            return;
        }
        if (other.getSummary() != null) {
            this.summary.addAll(other.getSummary());
        }
        if (other.getCharts() != null) {
            this.charts.addAll(other.getCharts());
        }
    }
}
