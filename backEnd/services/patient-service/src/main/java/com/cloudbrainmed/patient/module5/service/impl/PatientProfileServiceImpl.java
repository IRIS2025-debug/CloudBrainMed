package com.cloudbrainmed.patient.module5.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.patient.entity.Patient;
import com.cloudbrainmed.patient.mapper.PatientMapper;
import com.cloudbrainmed.patient.module5.service.PatientProfileService;
import org.springframework.http.HttpEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class PatientProfileServiceImpl implements PatientProfileService {

    private final PatientMapper patientMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${upload.avatar.patient-dir:${user.dir}/uploads/avatar/patient}")
    private String avatarUploadDir;
    @Value("${patient.sms.verify-url:http://localhost:8002/auth-service/patient/verify-code}")
    private String smsVerifyUrl;

    public PatientProfileServiceImpl(PatientMapper patientMapper) {
        this.patientMapper = patientMapper;
    }

    @Override
    public Patient getInfo(String patientId) {
        Patient patient = getPatientRaw(patientId);
        patient.setPhone(desensitizePhone(patient.getPhone()));
        patient.setIdCard(desensitizeIdCard(patient.getIdCard()));
        return patient;
    }

    @Override
    public Patient getInfoRaw(String patientId) {
        return getPatientRaw(patientId);
    }

    @Override
    public void updateInfo(String patientId, String name, String gender, String address, String birthday) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("患者不存在");
        if (name != null && !name.isEmpty()) patient.setName(name);
        if (gender != null && !gender.isEmpty()) patient.setGender(parseGender(gender));
        if (address != null) patient.setAddress(address);
        if (birthday != null && !birthday.isEmpty()) patient.setBirthday(parseBirthday(birthday));
        patientMapper.updateBasicInfo(patient);
    }

    @Override
    public String uploadAvatar(String patientId, byte[] fileBytes, String originalFilename) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("患者不存在");

        String ext = ".png";
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf(".");
            if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
                ext = originalFilename.substring(dotIndex);
            }
        }
        String filename = patientId + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(avatarUploadDir);
            Files.createDirectories(dir);
            Files.write(dir.resolve(filename), fileBytes);
        } catch (IOException e) {
            throw new BusinessException("头像上传失败，请稍后重试");
        }
        String avatarUrl = "/files/avatar/patient/" + filename;
        patientMapper.updateAvatar(patientId, avatarUrl);
        return avatarUrl;
    }

    @Override
    public void changePhone(String patientId, String oldPhone, String newPhone, String smsCode) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("患者不存在");
        if (!Objects.equals(patient.getPhone(), oldPhone)) throw new BusinessException("原手机号不正确");
        if (Objects.equals(newPhone, patient.getPhone())) return;
        if (patientMapper.selectByPhone(newPhone) != null) throw new BusinessException("新手机号已被使用");
        if (smsCode == null || smsCode.isBlank()) {
            throw new BusinessException("验证码不能为空");
        }
        try {
            Map<String, String> verifyReq = new HashMap<>();
            verifyReq.put("phone", newPhone);
            verifyReq.put("code", smsCode);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(
                    smsVerifyUrl,
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

        patientMapper.updatePhone(patientId, newPhone);
    }

    @Override
    public void changePassword(String patientId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new BusinessException("请输入原密码");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码至少6位");
        }
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("患者不存在");
        String oldEncrypted = encryptPassword(oldPassword);
        if (!patient.getPassword().equals(oldEncrypted)) throw new BusinessException("原密码不正确");
        patientMapper.updatePassword(patientId, encryptPassword(newPassword));
    }

    @Override
    public void verifyIdCard(String patientId, String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("请输入密码");
        }
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("患者不存在");
        String encrypted = encryptPassword(password);
        if (!patient.getPassword().equals(encrypted)) throw new BusinessException("密码验证失败");
    }

    @Override
    public void changeIdCard(String patientId, String newIdCard, String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("密码不能为空");
        }
        if (newIdCard == null || !newIdCard.matches("^\\d{17}[\\dXx]$")) {
            throw new BusinessException("身份证号格式不正确");
        }
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("患者不存在");
        String encrypted = encryptPassword(password);
        if (!patient.getPassword().equals(encrypted)) throw new BusinessException("密码错误");
        if (newIdCard.equals(patient.getIdCard())) {
            return;
        }
        if (patientMapper.selectByIdCard(newIdCard) != null) {
            throw new BusinessException("该身份证号已被使用");
        }
        patientMapper.updateIdCard(patientId, newIdCard);
    }

    private Integer parseGender(String gender) {
        if ("男".equals(gender) || "1".equals(gender)) return 1;
        if ("女".equals(gender) || "2".equals(gender)) return 2;
        throw new BusinessException("性别参数错误");
    }

    private LocalDate parseBirthday(String birthday) {
        try {
            return LocalDate.parse(birthday);
        } catch (DateTimeParseException e) {
            throw new BusinessException("生日格式错误");
        }
    }

    private Patient getPatientRaw(String patientId) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("患者不存在");
        patient.setPassword(null);
        return patient;
    }

    private String desensitizePhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String desensitizeIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
}
