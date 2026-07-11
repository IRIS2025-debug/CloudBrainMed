package com.cloudbrainmed.admin.service.impl;

import com.cloudbrainmed.admin.mapper.DataBoardMapper;
import com.cloudbrainmed.admin.service.DataBoardService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DataBoardServiceImpl implements DataBoardService {

    private final DataBoardMapper dataBoardMapper;

    public DataBoardServiceImpl(DataBoardMapper dataBoardMapper) {
        this.dataBoardMapper = dataBoardMapper;
    }

    @Override
    public Map<String, Integer> getOverview() {
        // 数据看板只返回 admin-service 自己拥有的数据（今日有效排班）。
        // AI 推理次数 / 活跃模型 / 训练样本由前端并行调用 ai-service 获取。
        Map<String, Integer> overview = new LinkedHashMap<>();
        overview.put("todayScheduleCount", dataBoardMapper.countTodaySchedules());
        return overview;
    }
}
