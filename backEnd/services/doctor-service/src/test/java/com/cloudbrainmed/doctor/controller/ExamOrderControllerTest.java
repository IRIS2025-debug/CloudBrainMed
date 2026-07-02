package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.service.ExamOrderService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExamOrderControllerTest {

    @Test
    void listByRegisterIdUsesDoctorIdFromTokenInsteadOfQueryParam() {
        ExamOrderService examOrderService = mock(ExamOrderService.class);
        ExamOrderController controller = new ExamOrderController(examOrderService);
        String token = DoctorJwtUtil.createToken("D001", "11111111111", 2, 1);

        controller.listByRegisterId(token, "R001", "D999");

        verify(examOrderService).getByRegisterId("R001", "D001");
    }
}
