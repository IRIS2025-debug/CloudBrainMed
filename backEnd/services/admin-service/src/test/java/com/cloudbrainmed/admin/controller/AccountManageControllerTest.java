package com.cloudbrainmed.admin.controller;

import com.cloudbrainmed.admin.service.AccountService;
import com.cloudbrainmed.admin.vo.AdminProfileVo;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountManageControllerTest {

    private final AccountService accountService = mock(AccountService.class);
    private final AccountManageController controller = new AccountManageController(accountService);

    @Test
    void infoDelegatesForAdminToken() {
        String token = DoctorJwtUtil.createToken("A001", "13700000001", 3);
        AdminProfileVo profile = new AdminProfileVo();
        profile.setAdminId("A001");
        when(accountService.getAdminInfo("A001")).thenReturn(profile);

        Object data = controller.info(token).getData();

        assertThat(data).isEqualTo(profile);
        verify(accountService).getAdminInfo("A001");
    }

    @Test
    void updateRejectsDoctorToken() {
        String token = DoctorJwtUtil.createToken("D001", "13900000001", 2, 1);

        assertThatThrownBy(() -> controller.update(token, Map.of("email", "a@b.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅管理员");
    }

    @Test
    void infoRejectsMissingTokenAsBusinessException() {
        assertThatThrownBy(() -> controller.info(null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void avatarUploadUsesAdminIdFromToken() throws IOException {
        String token = DoctorJwtUtil.createToken("A002", "13700000002", 3);
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[] {1, 2});
        when(accountService.uploadAvatar("A002", file.getBytes(), "avatar.png"))
                .thenReturn("/files/avatar/admin/a002.png");

        Object data = controller.avatarUpload(token, file).getData();

        assertThat(data).isEqualTo(Map.of("avatarUrl", "/files/avatar/admin/a002.png"));
        verify(accountService).uploadAvatar("A002", file.getBytes(), "avatar.png");
    }
}
