package com.cloudbrainmed.patient.module5.service.impl;

import com.cloudbrainmed.api.feign.AuthFeignClient;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.patient.entity.Patient;
import com.cloudbrainmed.patient.mapper.PatientMapper;
import com.cloudbrainmed.patient.module5.service.PatientProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class PatientProfileServiceImpl implements PatientProfileService {

    private static final int MAX_AVATAR_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private final PatientMapper patientMapper;
    private final AuthFeignClient authFeignClient;

    @Value("${upload.avatar.patient-dir:${user.dir}/uploads/avatar/patient}")
    private String avatarUploadDir;

    public PatientProfileServiceImpl(PatientMapper patientMapper, AuthFeignClient authFeignClient) {
        this.patientMapper = patientMapper;
        this.authFeignClient = authFeignClient;
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
        return getInfo(patientId);
    }

    @Override
    public void updateInfo(String patientId, String name, String gender, String address, String birthday) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("\u60a3\u8005\u4e0d\u5b58\u5728");
        if (name != null && !name.isEmpty()) patient.setName(name);
        if (gender != null && !gender.isEmpty()) patient.setGender(parseGender(gender));
        if (address != null) patient.setAddress(address);
        if (birthday != null && !birthday.isEmpty()) patient.setBirthday(parseBirthday(birthday));
        patientMapper.updateBasicInfo(patient);
    }

    @Override
    public String uploadAvatar(String patientId, byte[] fileBytes, String originalFilename) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("\u60a3\u8005\u4e0d\u5b58\u5728");
        validateAvatar(fileBytes, originalFilename);

        String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        String filename = patientId + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(avatarUploadDir);
            Files.createDirectories(dir);
            Files.write(dir.resolve(filename), fileBytes);
        } catch (IOException e) {
            throw new BusinessException("\u5934\u50cf\u4e0a\u4f20\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5");
        }
        String avatarUrl = "/files/avatar/patient/" + filename;
        patientMapper.updateAvatar(patientId, avatarUrl);
        return avatarUrl;
    }

    @Override
    public void changePhone(String patientId, String oldPhone, String newPhone, String smsCode) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("\u60a3\u8005\u4e0d\u5b58\u5728");
        if (!Objects.equals(patient.getPhone(), oldPhone)) throw new BusinessException("\u539f\u624b\u673a\u53f7\u4e0d\u6b63\u786e");
        if (Objects.equals(newPhone, patient.getPhone())) return;
        if (patientMapper.selectByPhone(newPhone) != null) throw new BusinessException("\u65b0\u624b\u673a\u53f7\u5df2\u88ab\u4f7f\u7528");
        if (smsCode == null || smsCode.isBlank()) {
            throw new BusinessException("\u9a8c\u8bc1\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        }
        try {
            Map<String, String> verifyReq = new HashMap<>();
            verifyReq.put("phone", newPhone);
            verifyReq.put("code", smsCode);
            Result<Map<String, Object>> resp = authFeignClient.verifyPatientCode(verifyReq);
            if (resp == null || !Integer.valueOf(200).equals(resp.getCode())) {
                throw new BusinessException("\u9a8c\u8bc1\u7801\u6821\u9a8c\u5931\u8d25");
            }
            Map<String, Object> dataMap = resp.getData();
            if (dataMap == null) {
                throw new BusinessException("\u9a8c\u8bc1\u7801\u6821\u9a8c\u5931\u8d25");
            }
            if (!Boolean.TRUE.equals(dataMap.get("valid"))) {
                throw new BusinessException("\u9a8c\u8bc1\u7801\u9519\u8bef\u6216\u5df2\u8fc7\u671f");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("\u9a8c\u8bc1\u7801\u6821\u9a8c\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5");
        }

        patientMapper.updatePhone(patientId, newPhone);
    }

    @Override
    public void changePassword(String patientId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new BusinessException("\u8bf7\u8f93\u5165\u539f\u5bc6\u7801");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("\u65b0\u5bc6\u7801\u81f3\u5c116\u4f4d");
        }
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("\u60a3\u8005\u4e0d\u5b58\u5728");
        String oldEncrypted = encryptPassword(oldPassword);
        if (!patient.getPassword().equals(oldEncrypted)) throw new BusinessException("\u539f\u5bc6\u7801\u4e0d\u6b63\u786e");
        patientMapper.updatePassword(patientId, encryptPassword(newPassword));
    }

    @Override
    public void verifyIdCard(String patientId, String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("\u8bf7\u8f93\u5165\u5bc6\u7801");
        }
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("\u60a3\u8005\u4e0d\u5b58\u5728");
        String encrypted = encryptPassword(password);
        if (!patient.getPassword().equals(encrypted)) throw new BusinessException("\u5bc6\u7801\u9a8c\u8bc1\u5931\u8d25");
    }

    @Override
    public void changeIdCard(String patientId, String newIdCard, String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("\u5bc6\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (newIdCard == null || !newIdCard.matches("^\\d{17}[\\dXx]$")) {
            throw new BusinessException("\u8eab\u4efd\u8bc1\u53f7\u683c\u5f0f\u4e0d\u6b63\u786e");
        }
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("\u60a3\u8005\u4e0d\u5b58\u5728");
        String encrypted = encryptPassword(password);
        if (!patient.getPassword().equals(encrypted)) throw new BusinessException("\u5bc6\u7801\u9519\u8bef");
        if (newIdCard.equals(patient.getIdCard())) {
            return;
        }
        if (patientMapper.selectByIdCard(newIdCard) != null) {
            throw new BusinessException("\u8be5\u8eab\u4efd\u8bc1\u53f7\u5df2\u88ab\u4f7f\u7528");
        }
        patientMapper.updateIdCard(patientId, newIdCard);
    }

    private void validateAvatar(byte[] fileBytes, String originalFilename) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BusinessException("\u5934\u50cf\u6587\u4ef6\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (fileBytes.length > MAX_AVATAR_BYTES) {
            throw new BusinessException("\u5934\u50cf\u6587\u4ef6\u4e0d\u80fd\u8d85\u8fc7 2MB");
        }
        if (originalFilename == null) {
            throw new BusinessException("\u5934\u50cf\u4ec5\u652f\u6301 jpg\u3001jpeg\u3001png\u3001gif\u3001webp \u683c\u5f0f");
        }
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            throw new BusinessException("\u5934\u50cf\u4ec5\u652f\u6301 jpg\u3001jpeg\u3001png\u3001gif\u3001webp \u683c\u5f0f");
        }
        String ext = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
        if (!ALLOWED_AVATAR_EXTENSIONS.contains(ext)) {
            throw new BusinessException("\u5934\u50cf\u4ec5\u652f\u6301 jpg\u3001jpeg\u3001png\u3001gif\u3001webp \u683c\u5f0f");
        }
    }

    private Integer parseGender(String gender) {
        if ("1".equals(gender) || "\u7537".equals(gender)) return 1;
        if ("2".equals(gender) || "\u5973".equals(gender)) return 2;
        throw new BusinessException("\u6027\u522b\u53c2\u6570\u9519\u8bef");
    }

    private LocalDate parseBirthday(String birthday) {
        try {
            return LocalDate.parse(birthday);
        } catch (DateTimeParseException e) {
            throw new BusinessException("\u751f\u65e5\u683c\u5f0f\u9519\u8bef");
        }
    }

    private Patient getPatientRaw(String patientId) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BusinessException("\u60a3\u8005\u4e0d\u5b58\u5728");
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
