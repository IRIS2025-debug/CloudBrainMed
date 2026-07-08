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
    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("doctorCount", dataBoardMapper.countDoctors());
        data.put("patientCount", dataBoardMapper.countPatients());
        data.put("todayRegistrationCount",
                dataBoardMapper.countTodayRegistrations());
        data.put("pendingMedicalOrderCount",
                dataBoardMapper.countPendingMedicalOrders());
        data.put("lowStockMedicineCount",
                dataBoardMapper.countLowStockMedicines());
        return data;
    }
}
