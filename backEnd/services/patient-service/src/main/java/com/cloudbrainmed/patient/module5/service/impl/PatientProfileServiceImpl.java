package com.cloudbrainmed.patient.module5.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.patient.entity.Patient;
import com.cloudbrainmed.patient.mapper.PatientMapper;
import com.cloudbrainmed.patient.module5.service.PatientProfileService;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class PatientProfileServiceImpl implements PatientProfileService {

    private final PatientMapper patientMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public PatientProfileServiceImpl(PatientMapper patientMapper) {
        this.patientMapper = patientMapper;
    }

    @Override
    public Patient getInfo(String patientId) {
        Patient p = getPatientRaw(patientId);
        p.setPhone(desensitizePhone(p.getPhone()));
        p.setIdCard(desensitizeIdCard(p.getIdCard()));
        return p;
    }

    /**
     * 查询个人完整信息（不脱敏），仅用于改手机号等需要原始手机号的场景
     */
    @Override
    public Patient getInfoRaw(String patientId) {
        return getPatientRaw(patientId);
    }

    @Override
    public void updateInfo(String patientId, String name, String gender, String address, String birthday) {
        Patient p = patientMapper.selectById(patientId);
        if (p == null) throw new BusinessException("患者不存在");
        if (name != null && !name.isEmpty()) p.setName(name);
        if (gender != null && !gender.isEmpty()) p.setGender(parseGender(gender));
        if (address != null) p.setAddress(address);
        if (birthday != null && !birthday.isEmpty()) p.setBirthday(java.time.LocalDate.parse(birthday));
        patientMapper.update(p);
    }

    @Override
    public String uploadAvatar(String patientId, byte[] fileBytes, String originalFilename) {
        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = "avatar/patient/" + patientId + "_" + System.currentTimeMillis() + ext;
        String avatarUrl = "/files/" + filename;
        return avatarUrl;
    }

    @Override
    public void changePhone(String patientId, String oldPhone, String newPhone, String smsCode) {
        // 1. 校验验证码
        if (smsCode == null || smsCode.isBlank()) {
            throw new BusinessException("验证码不能为空");
        }
        try {
            Map<String, String> verifyReq = new HashMap<>();
            verifyReq.put("phone", newPhone);
            verifyReq.put("code", smsCode);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(
                    "http://localhost:8002/auth-service/patient/verify-code",
                    new HttpEntity<>(verifyReq), Map.class);
            if (resp == null || !Integer.valueOf(200).equals(resp.get("code"))) {
                throw new BusinessException("验证码校验失败");
            }
            Object dataObj = resp.get("data");
            if (!(dataObj instanceof Map)) {
                throw new BusinessException("验证码校验失败");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) dataObj;
            if (!Boolean.TRUE.equals(dataMap.get("valid"))) {
                throw new BusinessException("验证码错误或已过期");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("验证码校验失败，请稍后重试");
        }

        // 2. 校验手机号
        Patient p = patientMapper.selectById(patientId);
        if (p == null) throw new BusinessException("患者不存在");
        if (!p.getPhone().equals(oldPhone)) throw new BusinessException("原手机号不正确");
        if (patientMapper.selectByPhone(newPhone) != null) throw new BusinessException("新手机号已被使用");
        p.setPhone(newPhone);
        patientMapper.update(p);
    }

    @Override
    public void changePassword(String patientId, String oldPassword, String newPassword) {
        Patient p = patientMapper.selectById(patientId);
        if (p == null) throw new BusinessException("患者不存在");
        String oldEncrypted = DigestUtils.md5DigestAsHex(oldPassword.getBytes(StandardCharsets.UTF_8));
        if (!p.getPassword().equals(oldEncrypted)) throw new BusinessException("原密码不正确");
        String newEncrypted = DigestUtils.md5DigestAsHex(newPassword.getBytes(StandardCharsets.UTF_8));
        patientMapper.updatePassword(patientId, newEncrypted);
    }

    @Override
    public void verifyIdCard(String patientId, String password) {
        Patient p = patientMapper.selectById(patientId);
        if (p == null) throw new BusinessException("患者不存在");
        String encrypted = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!p.getPassword().equals(encrypted)) throw new BusinessException("密码验证失败");
    }

    private Integer parseGender(String gender) {
        if ("男".equals(gender) || "1".equals(gender)) return 1;
        if ("女".equals(gender) || "2".equals(gender)) return 2;
        throw new BusinessException("性别参数错误");
    }

    /**
     * 查库返回患者完整信息（仅去密码，不脱敏）。
     * 警告：返回值是数据库实体，新增 Patient 敏感字段时需确认本方法及调用方是否需跳过该字段
     */
    private Patient getPatientRaw(String patientId) {
        Patient p = patientMapper.selectById(patientId);
        if (p == null) throw new BusinessException("患者不存在");
        p.setPassword(null);
        return p;
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
