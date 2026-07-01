package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.entity.Dept;
import com.cloudbrainmed.doctor.entity.Doctor;
import com.cloudbrainmed.doctor.mapper.DeptMapper;
import com.cloudbrainmed.doctor.mapper.DoctorMapper;
import com.cloudbrainmed.doctor.service.DoctorService;
import com.cloudbrainmed.doctor.vo.DoctorProfileVo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@Service
public class DoctorServiceImpl implements DoctorService {
    @Resource
    private DoctorMapper doctorMapper;
    @Resource
    private DeptMapper deptMapper;
    @Value("${upload.avatar.doctor-dir:${user.dir}/uploads/avatar/doctor}")
    private String avatarUploadDir;

    @Override
    public DoctorProfileVo getDoctorInfo(String doctorId) {
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor == null) {
            throw new BusinessException("医生信息不存在");
        }

        DoctorProfileVo vo = new DoctorProfileVo();
        vo.setName(doctor.getName());
        vo.setPosition(doctor.getPosition());
        vo.setAvatar(doctor.getAvatar());
        vo.setGoodAt(doctor.getGoodAt());
        vo.setIntroduction(doctor.getIntroduction());
        vo.setEmail(doctor.getEmail());

        if (doctor.getDepartmentId() != null) {
            Dept dept = deptMapper.selectById(doctor.getDepartmentId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }

        return vo;
    }

    @Override
    public void updateDoctorProfile(String doctorId, String goodAt, String introduction, String email) {
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor == null) {
            throw new BusinessException("医生信息不存在");
        }
        if (goodAt != null) doctor.setGoodAt(goodAt);
        if (introduction != null) doctor.setIntroduction(introduction);
        if (email != null) doctor.setEmail(email);
        doctorMapper.updateById(doctor);
    }

    @Override
    public String uploadAvatar(String doctorId, byte[] fileBytes, String originalFilename) {
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor == null) {
            throw new BusinessException("医生信息不存在");
        }

        String ext = ".png";
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf(".");
            if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
                ext = originalFilename.substring(dotIndex);
            }
        }
        String filename = doctorId + "_" + UUID.randomUUID().toString().replace("-", "") + ext;

        try {
            Path dir = Paths.get(avatarUploadDir);
            Files.createDirectories(dir);
            Files.write(dir.resolve(filename), fileBytes);
        } catch (IOException e) {
            throw new BusinessException("头像上传失败，请稍后重试");
        }

        String avatarUrl = "/files/avatar/doctor/" + filename;
        doctor.setAvatar(avatarUrl);
        doctorMapper.updateById(doctor);
        return avatarUrl;
    }

    @Override
    public void changePhone(String doctorId, String oldPhone, String newPhone) {
        if (newPhone == null || newPhone.isBlank()) {
            throw new BusinessException("请输入新手机号");
        }
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor == null) {
            throw new BusinessException("医生信息不存在");
        }
        if (!Objects.equals(doctor.getPhone(), oldPhone)) {
            throw new BusinessException("原手机号不正确");
        }
        String trimmedNewPhone = newPhone.trim();
        if (Objects.equals(oldPhone, trimmedNewPhone)) {
            throw new BusinessException("新手机号不能与原手机号相同");
        }
        doctorMapper.updatePhone(doctorId, trimmedNewPhone);
    }

    @Override
    public void changePassword(String doctorId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new BusinessException("请输入原密码");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码至少6位");
        }
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor == null) {
            throw new BusinessException("医生信息不存在");
        }
        String currentPassword = doctor.getPassword();
        String oldEncrypted = encryptPassword(oldPassword);
        boolean matchesPlain = oldPassword.equals(currentPassword);
        boolean matchesEncrypted = oldEncrypted.equals(currentPassword);
        if (!matchesPlain && !matchesEncrypted) {
            throw new BusinessException("原密码不正确");
        }
        doctor.setPassword(encryptPassword(newPassword));
        doctorMapper.updateById(doctor);
    }

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
}
