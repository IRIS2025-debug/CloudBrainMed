package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.vo.DoctorProfileVo;

public interface DoctorService {
    DoctorProfileVo getDoctorInfo(String doctorId);
    void updateDoctorProfile(String doctorId, String goodAt, String introduction, String email);
    String uploadAvatar(String doctorId, byte[] fileBytes, String originalFilename);
    void changePhone(String doctorId, String oldPhone, String newPhone);
    void changePassword(String doctorId, String oldPassword, String newPassword);
}
