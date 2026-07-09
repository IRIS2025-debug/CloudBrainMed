package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.service.MedicalOrderService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InspectionDoctorControllerTest {

    private final MedicalOrderService medicalOrderService =
            mock(MedicalOrderService.class);
    private final InspectionDoctorController controller =
            new InspectionDoctorController(medicalOrderService);

    @Test
    void ordersAllowsExaminationDoctor() {
        String token = DoctorJwtUtil.createToken("D002", "11111111111", 2, 2);

        assertDoesNotThrow(() -> controller.getAllLabOrders(token));

        verify(medicalOrderService).getAllLabOrders();
    }

    @Test
    void ordersAllowsLabDoctor() {
        String token = DoctorJwtUtil.createToken("D003", "11111111111", 2, 3);

        assertDoesNotThrow(() -> controller.getAllLabOrders(token));

        verify(medicalOrderService).getAllLabOrders();
    }

    @Test
    void ordersRejectsReceptionDoctor() {
        String token = DoctorJwtUtil.createToken("D001", "11111111111", 2, 1);

        assertThrows(RuntimeException.class,
                () -> controller.getAllLabOrders(token));
    }
}
