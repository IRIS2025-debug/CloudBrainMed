package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.doctor.service.DoctorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DoctorProfileControllerTest {

    @Test
    void changePhoneDelegatesToDoctorService() {
        DoctorService doctorService = mock(DoctorService.class);
        DoctorProfileController controller = new DoctorProfileController();
        ReflectionTestUtils.setField(controller, "doctorService", doctorService);

        controller.changePhone("D001", Map.of(
                "oldPhone", "13800000000",
                "newPhone", "13900000000"));

        verify(doctorService).changePhone("D001", "13800000000", "13900000000");
    }
}
