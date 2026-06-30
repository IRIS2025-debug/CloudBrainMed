package com.cloudbrainmed.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudbrainmed.auth.dto.LoginDto;
import com.cloudbrainmed.auth.entity.Admin;
import com.cloudbrainmed.auth.entity.Doctor;
import com.cloudbrainmed.auth.mapper.AdminMapper;
import com.cloudbrainmed.auth.mapper.DoctorMapper;
import com.cloudbrainmed.auth.service.DoctorAuthService;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Data
@Service
public class DoctorAuthServiceImpl implements DoctorAuthService {
    @Resource
    private DoctorMapper doctorMapper;
    @Resource
    private AdminMapper adminMapper;

    public Map<String, Object> login(LoginDto dto) {
        String phone = dto.getPhone();
        String password = dto.getPassword();
        Integer role = dto.getRoleType();

        if (role == null) throw new BusinessException("请选择登录角色");
        if (role == 2) {
            Doctor doctor = doctorMapper.selectOne(new LambdaQueryWrapper<Doctor>()
                    .eq(Doctor::getPhone, phone)
                    .and(wrapper -> wrapper
                            .eq(Doctor::getPassword, password)
                            .or()
                            .eq(Doctor::getPassword, encryptPassword(password))));
            if (doctor == null) throw new BusinessException("医生账号或密码错误");
            String token = DoctorJwtUtil.createToken(doctor.getDoctorId(), phone, role);
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("roleType", role);
            result.put("doctorType", doctor.getDoctorType() != null ? doctor.getDoctorType() : 1);
            return result;
        } else if (role == 3) {
            Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                    .eq(Admin::getPhone, phone)
                    .and(wrapper -> wrapper
                            .eq(Admin::getPassword, password)
                            .or()
                            .eq(Admin::getPassword, encryptPassword(password))));
            if (admin == null) throw new BusinessException("管理员账号或密码错误");
            String token = DoctorJwtUtil.createToken(admin.getAdminId(), phone, role);
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("roleType", role);
            return result;
        }
        throw new BusinessException("角色类型错误");
    }

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
}
