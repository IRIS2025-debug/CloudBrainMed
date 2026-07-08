package com.cloudbrainmed.admin.controller;

import com.cloudbrainmed.admin.service.DoctorManageService;
import com.cloudbrainmed.admin.support.AdminAuthHelper;
import com.cloudbrainmed.admin.vo.DoctorManageVo;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserManageControllerTest {

    private final DoctorManageService doctorManageService =
            mock(DoctorManageService.class);
    private final UserManageController controller = newController();

    @Test
    void listRejectsMissingToken() {
        assertThatThrownBy(() -> controller.list(null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void listAllowsAdminToken() {
        String adminToken = DoctorJwtUtil.createToken(
                "A001", "13700000001", 3);
        DoctorManageVo doctor = new DoctorManageVo();
        doctor.setDoctorId("D001");
        when(doctorManageService.listAll()).thenReturn(List.of(doctor));

        Object data = controller.list(adminToken).getData();

        assertThat(data).isEqualTo(List.of(doctor));
        verify(doctorManageService).listAll();
    }

    private UserManageController newController() {
        UserManageController controller = new UserManageController();
        ReflectionTestUtils.setField(
                controller, "doctorManageService", doctorManageService);
        ReflectionTestUtils.setField(
                controller, "adminAuthHelper", new AdminAuthHelper());
        return controller;
    }
}
