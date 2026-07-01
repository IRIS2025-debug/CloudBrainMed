package com.cloudbrainmed.patient.module5.service.impl;

import com.cloudbrainmed.api.feign.AuthFeignClient;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.patient.entity.Patient;
import com.cloudbrainmed.patient.mapper.PatientMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatientProfileServiceImplTest {

    private PatientMapper patientMapper;
    private AuthFeignClient authFeignClient;
    private PatientProfileServiceImpl service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        patientMapper = mock(PatientMapper.class);
        authFeignClient = mock(AuthFeignClient.class);
        service = new PatientProfileServiceImpl(patientMapper, authFeignClient);
        ReflectionTestUtils.setField(service, "avatarUploadDir", tempDir.toString());
    }

    @Test
    void changePasswordRejectsBlankPasswordWithoutNullPointer() {
        Patient patient = new Patient();
        patient.setPatientId("P001");
        patient.setPassword(encrypt("old123"));
        when(patientMapper.selectById("P001")).thenReturn(patient);

        assertThatThrownBy(() -> service.changePassword("P001", null, "new123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请输入原密码");
        assertThatThrownBy(() -> service.changePassword("P001", "old123", ""))
                .isInstanceOf(BusinessException.class)
                .hasMessage("新密码至少6位");
    }

    @Test
    void uploadAvatarStoresFileAndUpdatesPatientAvatar() throws Exception {
        Patient patient = new Patient();
        patient.setPatientId("P001");
        when(patientMapper.selectById("P001")).thenReturn(patient);

        String avatarUrl = service.uploadAvatar("P001", new byte[] {1}, "avatar.png");

        assertThat(avatarUrl).startsWith("/files/avatar/patient/P001_");
        assertThat(avatarUrl).endsWith(".png");
        assertThat(Files.list(tempDir)).anyMatch(path -> path.getFileName().toString().endsWith(".png"));
        verify(patientMapper).updateAvatar("P001", avatarUrl);
    }

    @Test
    void uploadAvatarRejectsUnsupportedExtension() {
        Patient patient = new Patient();
        patient.setPatientId("P001");
        when(patientMapper.selectById("P001")).thenReturn(patient);

        assertThatThrownBy(() -> service.uploadAvatar("P001", new byte[] {1}, "avatar.exe"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("头像仅支持 jpg、jpeg、png、gif、webp 格式");
    }

    @Test
    void uploadAvatarRejectsOversizedFile() {
        Patient patient = new Patient();
        patient.setPatientId("P001");
        when(patientMapper.selectById("P001")).thenReturn(patient);
        byte[] oversized = new byte[2 * 1024 * 1024 + 1];

        assertThatThrownBy(() -> service.uploadAvatar("P001", oversized, "avatar.png"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("头像文件不能超过 2MB");
    }

    @Test
    void getInfoRawDoesNotExposeFullPhoneOrIdCard() {
        Patient patient = new Patient();
        patient.setPatientId("P001");
        patient.setPhone("13800000000");
        patient.setIdCard("110101199001011234");
        patient.setPassword(encrypt("old123"));
        when(patientMapper.selectById("P001")).thenReturn(patient);

        Patient result = service.getInfoRaw("P001");

        assertThat(result.getPassword()).isNull();
        assertThat(result.getPhone()).isEqualTo("138****0000");
        assertThat(result.getIdCard()).isEqualTo("1101**********1234");
    }

    @Test
    void uploadAvatarUsesReadableMessageWhenPatientMissing() {
        when(patientMapper.selectById("P404")).thenReturn(null);

        assertThatThrownBy(() -> service.uploadAvatar("P404", new byte[] {1}, "avatar.png"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("患者不存在");
    }

    @Test
    void updateInfoRejectsInvalidBirthdayWithBusinessMessage() {
        Patient patient = new Patient();
        patient.setPatientId("P001");
        when(patientMapper.selectById("P001")).thenReturn(patient);

        assertThatThrownBy(() -> service.updateInfo("P001", null, null, null, "2026/06/29"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("生日格式错误");

        verify(patientMapper, never()).update(patient);
    }

    @Test
    void updateInfoOnlyUpdatesBasicProfileFields() {
        Patient patient = new Patient();
        patient.setPatientId("P001");
        when(patientMapper.selectById("P001")).thenReturn(patient);

        service.updateInfo("P001", "张三", "1", "上海", "1990-01-01");

        verify(patientMapper).updateBasicInfo(patient);
        verify(patientMapper, never()).update(patient);
    }

    @Test
    void changePhoneRejectsMissingCurrentPhoneWithoutNullPointer() {
        Patient patient = new Patient();
        patient.setPatientId("P001");
        patient.setPhone(null);
        when(patientMapper.selectById("P001")).thenReturn(patient);

        assertThatThrownBy(() -> service.changePhone("P001", "13800000000", "13900000000", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("原手机号不正确");

        verify(patientMapper, never()).update(patient);
    }

    @Test
    void changePhoneOnlyUpdatesPhoneAfterValidation() {
        Patient patient = new Patient();
        patient.setPatientId("P001");
        patient.setPhone("13800000000");
        when(patientMapper.selectById("P001")).thenReturn(patient);
        when(patientMapper.selectByPhone("13900000000")).thenReturn(null);
        stubSmsVerifySuccess();

        service.changePhone("P001", "13800000000", "13900000000", "123456");

        verify(patientMapper).updatePhone("P001", "13900000000");
        verify(patientMapper, never()).update(patient);
    }

    @Test
    void changeIdCardOnlyUpdatesIdCardAfterValidation() {
        Patient patient = new Patient();
        patient.setPatientId("P001");
        patient.setPassword(encrypt("old123"));
        patient.setIdCard("110101199001011234");
        when(patientMapper.selectById("P001")).thenReturn(patient);
        when(patientMapper.selectByIdCard("110101199001011235")).thenReturn(null);

        service.changeIdCard("P001", "110101199001011235", "old123");

        verify(patientMapper).updateIdCard("P001", "110101199001011235");
        verify(patientMapper, never()).update(patient);
    }

    private String encrypt(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }

    private void stubSmsVerifySuccess() {
        when(authFeignClient.verifyPatientCode(Map.of("phone", "13900000000", "code", "123456")))
                .thenReturn(Result.ok(Map.of("valid", true)));
    }
}
