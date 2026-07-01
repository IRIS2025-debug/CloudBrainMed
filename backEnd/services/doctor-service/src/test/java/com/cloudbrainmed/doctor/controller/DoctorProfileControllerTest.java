package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.doctor.service.DoctorService;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DoctorProfileControllerTest {

    @Test
    void changePhoneDelegatesToDoctorService() {
        DoctorService doctorService = mock(DoctorService.class);
        DoctorProfileController controller = new DoctorProfileController();
        ReflectionTestUtils.setField(controller, "doctorService", doctorService);
        String doctorToken = DoctorJwtUtil.createToken("D001", "13800000000", 2);

        controller.changePhone(doctorToken, Map.of(
                "oldPhone", "13800000000",
                "newPhone", "13900000000"));

        verify(doctorService).changePhone("D001", "13800000000", "13900000000");
    }

    @Test
    void changePhoneRejectsInvalidToken() {
        DoctorService doctorService = mock(DoctorService.class);
        DoctorProfileController controller = new DoctorProfileController();
        ReflectionTestUtils.setField(controller, "doctorService", doctorService);

        assertThrows(RuntimeException.class, () -> controller.changePhone("D001", Map.of(
                "oldPhone", "13800000000",
                "newPhone", "13900000000")));

        verify(doctorService, never()).changePhone("D001", "13800000000", "13900000000");
    }

    @Test
    void changePhoneRejectsNonDoctorToken() {
        DoctorService doctorService = mock(DoctorService.class);
        DoctorProfileController controller = new DoctorProfileController();
        ReflectionTestUtils.setField(controller, "doctorService", doctorService);
        String patientToken = DoctorJwtUtil.createToken("P001", "13800000000", 1);

        assertThrows(RuntimeException.class, () -> controller.changePhone(patientToken, Map.of(
                "oldPhone", "13800000000",
                "newPhone", "13900000000")));

        verify(doctorService, never()).changePhone("P001", "13800000000", "13900000000");
    }
}
