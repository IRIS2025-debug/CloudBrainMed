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
        Map<String, Integer> overview = new LinkedHashMap<>();
        overview.put("doctorCount", dataBoardMapper.countDoctors());
        overview.put("departmentCount", dataBoardMapper.countDepartments());
        overview.put("todayScheduleCount", dataBoardMapper.countTodaySchedules());
        overview.put("medicineCount", dataBoardMapper.countMedicines());
        return overview;
    }
}
