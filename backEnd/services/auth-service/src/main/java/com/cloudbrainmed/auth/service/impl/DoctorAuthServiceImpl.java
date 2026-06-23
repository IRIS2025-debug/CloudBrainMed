package com.cloudbrainmed.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudbrainmed.auth.dto.LoginDto;
import com.cloudbrainmed.auth.entity.Admin;
import com.cloudbrainmed.auth.entity.Doctor;
import com.cloudbrainmed.auth.mapper.AdminMapper;
import com.cloudbrainmed.auth.mapper.DoctorMapper;
import com.cloudbrainmed.auth.service.AuthService;
import com.cloudbrainmed.auth.service.DoctorAuthService;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.common.utils.JwtUtil;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;

@Data
@Service
public class DoctorAuthServiceImpl implements DoctorAuthService {
    @Resource
    private DoctorMapper doctorMapper;
    @Resource
    private AdminMapper adminMapper;
    public String login(LoginDto dto) {
        String phone = dto.getPhone();
        String password = dto.getPassword();
        Integer role = dto.getRoleType();

        if (role == null) throw new BusinessException("请选择登录角色");
        if (role == 2) {
            Doctor d = doctorMapper.selectOne(new LambdaQueryWrapper<Doctor>()
                    .eq(Doctor::getPhone, phone)
                    .eq(Doctor::getPassword, password));
            if (d == null) throw new BusinessException("医生账号或密码错误");
            return DoctorJwtUtil.createToken(d.getDoctorId().toString(), phone, role);}
        else if (role == 3) {
            Admin a = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                    .eq(Admin::getPhone, phone)
                    .eq(Admin::getPassword, password));
            if (a == null) throw new BusinessException("管理员账号或密码错误");
            return DoctorJwtUtil.createToken(a.getAdminId().toString(), phone, role);
        }throw new BusinessException("角色类型错误");
    }
}