package com.cloudbrainmed.patient.module5.service;

import com.cloudbrainmed.patient.entity.Patient;

public interface PatientProfileService {
    Patient getInfo(String patientId);
    Patient getInfoRaw(String patientId);
    void updateInfo(String patientId, String name, String gender, String address, String birthday);
    String uploadAvatar(String patientId, byte[] fileBytes, String originalFilename);
    void changePhone(String patientId, String oldPhone, String newPhone, String smsCode);
    void changePassword(String patientId, String oldPassword, String newPassword);
    void verifyIdCard(String patientId, String password);
}
