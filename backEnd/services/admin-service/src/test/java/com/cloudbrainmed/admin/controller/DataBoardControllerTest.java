package com.cloudbrainmed.admin.controller;

import com.cloudbrainmed.admin.service.DataBoardService;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataBoardControllerTest {

    private final DataBoardService dataBoardService = mock(DataBoardService.class);
    private final DataBoardController controller = new DataBoardController(dataBoardService);

    @Test
    void returnsOverviewForAdminToken() {
        Map<String, Integer> overview = Map.of("todayScheduleCount", 8);
        when(dataBoardService.getOverview()).thenReturn(overview);

        Result<?> result = controller.overview(
                DoctorJwtUtil.createToken("A001", "13800000000", 3));

        assertEquals(200, result.getCode());
        assertEquals(overview, result.getData());
        verify(dataBoardService).getOverview();
    }

    @Test
    void rejectsMissingToken() {
        Result<?> result = controller.overview(" ");

        assertEquals(401, result.getCode());
        assertNull(result.getData());
        verify(dataBoardService, never()).getOverview();
    }

    @Test
    void rejectsInvalidToken() {
        Result<?> result = controller.overview("not-a-jwt");

        assertEquals(401, result.getCode());
        assertNull(result.getData());
        verify(dataBoardService, never()).getOverview();
    }

    @Test
    void rejectsNonAdminToken() {
        Result<?> result = controller.overview(
                DoctorJwtUtil.createToken("D001", "13800000001", 2));

        assertEquals(403, result.getCode());
        assertNull(result.getData());
        verify(dataBoardService, never()).getOverview();
    }
}
