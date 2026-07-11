package com.cloudbrainmed.admin.service.impl;

import com.cloudbrainmed.admin.mapper.DataBoardMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataBoardServiceImplTest {

    @Test
    void returnsOnlyAdminOwnedTodaySchedule() {
        DataBoardMapper mapper = mock(DataBoardMapper.class);
        when(mapper.countTodaySchedules()).thenReturn(8);

        Map<String, Integer> overview = new DataBoardServiceImpl(mapper).getOverview();

        assertEquals(Map.of("todayScheduleCount", 8), overview);
    }
}
