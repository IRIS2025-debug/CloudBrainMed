package com.cloudbrainmed.admin.service.impl;

import com.cloudbrainmed.admin.mapper.DataBoardMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataBoardServiceImplTest {

    @Test
    void combinesOwnedAdminStatistics() {
        DataBoardMapper mapper = mock(DataBoardMapper.class);
        when(mapper.countDoctors()).thenReturn(12);
        when(mapper.countDepartments()).thenReturn(4);
        when(mapper.countTodaySchedules()).thenReturn(8);
        when(mapper.countMedicines()).thenReturn(36);

        Map<String, Integer> overview = new DataBoardServiceImpl(mapper).getOverview();

        assertEquals(Map.of(
                "doctorCount", 12,
                "departmentCount", 4,
                "todayScheduleCount", 8,
                "medicineCount", 36), overview);
    }
}
