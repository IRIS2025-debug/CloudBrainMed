package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.DateValueStat;
import com.cloudbrainmed.ai.dto.NameValueStat;
import com.cloudbrainmed.ai.service.ChartDataBuilderService;
import com.cloudbrainmed.ai.vo.ChartData;
import com.cloudbrainmed.ai.vo.SeriesData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChartDataBuilderServiceImpl implements ChartDataBuilderService {

    private static final DateTimeFormatter MM_DD = DateTimeFormatter.ofPattern("MM-dd");

    @Override
    public ChartData statistic(String title, Object value) {
        ChartData chart = new ChartData();
        chart.setType("statistic");
        chart.setTitle(title);
        chart.setValue(value);
        return chart;
    }

    @Override
    public ChartData pie(String title, List<NameValueStat> data) {
        ChartData chart = new ChartData();
        chart.setType("pie");
        chart.setTitle(title);
        List<Map<String, Object>> pieData = new ArrayList<>();
        for (NameValueStat item : safeNameValue(data)) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("name", item.getName());
            point.put("value", normalizeNumber(item.getValue()));
            pieData.add(point);
        }
        chart.setData(pieData);
        return chart;
    }

    @Override
    public ChartData bar(String title, String seriesName, List<NameValueStat> data) {
        ChartData chart = new ChartData();
        chart.setType("bar");
        chart.setTitle(title);
        List<String> xAxis = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (NameValueStat item : safeNameValue(data)) {
            xAxis.add(item.getName());
            values.add(normalizeNumber(item.getValue()));
        }
        chart.setXAxis(xAxis);
        chart.setSeries(List.of(new SeriesData(seriesName, values)));
        return chart;
    }

    @Override
    public ChartData line(String title, String seriesName, List<DateValueStat> data) {
        ChartData chart = new ChartData();
        chart.setType("line");
        chart.setTitle(title);
        List<String> xAxis = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        if (data != null) {
            for (DateValueStat item : data) {
                xAxis.add(item.getStatDate() == null ? "未知" : MM_DD.format(item.getStatDate()));
                values.add(normalizeNumber(item.getValue()));
            }
        }
        chart.setXAxis(xAxis);
        chart.setSeries(List.of(new SeriesData(seriesName, values)));
        return chart;
    }

    private List<NameValueStat> safeNameValue(List<NameValueStat> data) {
        return data == null ? List.of() : data;
    }

    private Object normalizeNumber(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() <= 0) {
            return stripped.longValue();
        }
        return stripped;
    }
}
