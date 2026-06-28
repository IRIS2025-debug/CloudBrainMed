package org.example.backend.huanzhe.service;

import org.example.backend.huanzhe.entity.Patient;

public interface PatientProfileService {
    Patient getInfo(String patientId);
    /** 更新基础信息。性别和生日由身份证自动推导，不通过本接口修改 */
    void updateInfo(String patientId, String name, String address);
    String uploadAvatar(String patientId, byte[] fileBytes, String originalFilename);
    void changePhone(String patientId, String oldPhone, String newPhone, String smsCode);
    void changePassword(String patientId, String oldPassword, String newPassword);
    void verifyIdCard(String patientId, String password);
}
