package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.DateValueStat;
import com.cloudbrainmed.ai.dto.NameValueStat;
import com.cloudbrainmed.ai.vo.ChartData;

import java.util.List;

public interface ChartDataBuilderService {
    ChartData statistic(String title, Object value);

    ChartData pie(String title, List<NameValueStat> data);

    ChartData bar(String title, String seriesName, List<NameValueStat> data);

    ChartData line(String title, String seriesName, List<DateValueStat> data);
}
