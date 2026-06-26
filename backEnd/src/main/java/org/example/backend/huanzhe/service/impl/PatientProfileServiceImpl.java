package org.example.backend.huanzhe.service.impl;

import org.example.backend.common.BusinessException;
import org.example.backend.huanzhe.entity.Patient;
import org.example.backend.huanzhe.mapper.PatientMapper;
import org.example.backend.huanzhe.service.PatientProfileService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Service
public class PatientProfileServiceImpl implements PatientProfileService {

    private final PatientMapper patientMapper;

    public PatientProfileServiceImpl(PatientMapper patientMapper) {
        this.patientMapper = patientMapper;
    }

    @Override
    public Patient getInfo(String patientId) {
        Patient p = patientMapper.findById(patientId);
        if (p == null) throw new BusinessException("患者不存在");
        p.setPhone(desensitizePhone(p.getPhone()));
        p.setIdCard(desensitizeIdCard(p.getIdCard()));
        p.setPassword(null);
        return p;
    }

    @Override
    public void updateInfo(String patientId, String name, String address) {
        Patient p = patientMapper.findById(patientId);
        if (p == null) throw new BusinessException("患者不存在");

        if (name != null && !name.isEmpty()) p.setName(name);
        if (address != null) p.setAddress(address);
        // gender 和 birthday 不从接口接收 —— 由注册和改身份证时从身份证号自动推导
        if (p.getName() == null && p.getAddress() == null)
            throw new BusinessException("至少填写一项更新内容");

        p.setPatientId(patientId);
        patientMapper.updateInfo(p);
    }

    @Override
    public String uploadAvatar(String patientId, byte[] fileBytes, String originalFilename) {
        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = "avatar/patient/" + patientId + "_" + System.currentTimeMillis() + ext;
        // TODO: 对接 MinIO 后替换为真实上传
        String avatarUrl = "/files/" + filename;
        patientMapper.updateAvatar(patientId, avatarUrl);
        return avatarUrl;
    }

    @Override
    public void changePhone(String patientId, String oldPhone, String newPhone, String smsCode) {
        if (patientMapper.checkPhone(patientId, oldPhone) == null)
            throw new BusinessException("原手机号不正确");
        if (patientMapper.findByPhone(newPhone) != null)
            throw new BusinessException("新手机号已被使用");
        // TODO: 校验短信验证码
        patientMapper.updatePhone(patientId, newPhone);
    }

    @Override
    public void changePassword(String patientId, String oldPassword, String newPassword) {
        String stored = patientMapper.findPasswordById(patientId);
        if (stored == null) throw new BusinessException("患者不存在");
        String oldEncrypted = DigestUtils.md5DigestAsHex(oldPassword.getBytes(StandardCharsets.UTF_8));
        if (!stored.equals(oldEncrypted)) throw new BusinessException("原密码不正确");
        String newEncrypted = DigestUtils.md5DigestAsHex(newPassword.getBytes(StandardCharsets.UTF_8));
        patientMapper.updatePassword(patientId, newEncrypted);
    }

    @Override
    public void verifyIdCard(String patientId, String password) {
        String stored = patientMapper.findPasswordById(patientId);
        if (stored == null) throw new BusinessException("患者不存在");
        String encrypted = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!stored.equals(encrypted)) throw new BusinessException("密码验证失败");
    }

    private String desensitizePhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String desensitizeIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }
}
