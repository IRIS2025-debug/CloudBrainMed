package com.cloudbrainmed.admin.controller;

import com.cloudbrainmed.admin.service.DataBoardService;
import com.cloudbrainmed.admin.support.AdminAuthHelper;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataBoardControllerTest {

    private final DataBoardService dataBoardService =
            mock(DataBoardService.class);
    private final DataBoardController controller =
            new DataBoardController(dataBoardService, new AdminAuthHelper());

    @Test
    void overviewRequiresAdminToken() {
        String doctorToken = DoctorJwtUtil.createToken(
                "D001", "13900000001", 2, 1);

        assertThatThrownBy(() -> controller.overview(doctorToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅管理员");
    }

    @Test
    void overviewReturnsServiceDataForAdmin() {
        String adminToken = DoctorJwtUtil.createToken(
                "A001", "13700000001", 3);
        Map<String, Object> overview = Map.of("doctorCount", 3L);
        when(dataBoardService.overview()).thenReturn(overview);

        Object data = controller.overview(adminToken).getData();

        assertThat(data).isEqualTo(overview);
        verify(dataBoardService).overview();
    }
}
