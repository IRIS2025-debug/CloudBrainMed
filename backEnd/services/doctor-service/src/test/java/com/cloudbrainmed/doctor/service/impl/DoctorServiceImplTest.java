package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.entity.Doctor;
import com.cloudbrainmed.doctor.mapper.DeptMapper;
import com.cloudbrainmed.doctor.mapper.DoctorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorServiceImplTest {

    private DoctorMapper doctorMapper;
    private DoctorServiceImpl service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        doctorMapper = mock(DoctorMapper.class);
        service = new DoctorServiceImpl();
        ReflectionTestUtils.setField(service, "doctorMapper", doctorMapper);
        ReflectionTestUtils.setField(service, "deptMapper", mock(DeptMapper.class));
        ReflectionTestUtils.setField(service, "avatarUploadDir", tempDir.toString());
    }

    @Test
    void uploadAvatarStoresFileAndUpdatesDoctorAvatar() throws Exception {
        Doctor doctor = new Doctor();
        doctor.setDoctorId("D001");
        when(doctorMapper.selectById("D001")).thenReturn(doctor);

        String avatarUrl = service.uploadAvatar("D001", new byte[] {1, 2, 3}, "avatar");

        assertThat(avatarUrl).startsWith("/files/avatar/doctor/D001_");
        assertThat(avatarUrl).endsWith(".png");
        assertThat(Files.list(tempDir)).anyMatch(path -> path.getFileName().toString().endsWith(".png"));
        verify(doctorMapper).updateById(argThat((Doctor updated) -> avatarUrl.equals(updated.getAvatar())));
    }

    @Test
    void changePhoneUpdatesDoctorPhoneWhenOldPhoneMatches() {
        Doctor doctor = new Doctor();
        doctor.setDoctorId("D001");
        doctor.setPhone("13800000000");
        when(doctorMapper.selectById("D001")).thenReturn(doctor);

        service.changePhone("D001", "13800000000", "13900000000");

        verify(doctorMapper).updatePhone("D001", "13900000000");
    }
}
