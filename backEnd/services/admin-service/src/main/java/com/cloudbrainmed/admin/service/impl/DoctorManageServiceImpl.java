package com.cloudbrainmed.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudbrainmed.admin.entity.DeptInfo;
import com.cloudbrainmed.admin.entity.DoctorManage;
import com.cloudbrainmed.admin.mapper.DeptInfoMapper;
import com.cloudbrainmed.admin.mapper.DoctorManageMapper;
import com.cloudbrainmed.admin.service.DoctorManageService;
import com.cloudbrainmed.admin.vo.DoctorManageVo;
import com.cloudbrainmed.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class DoctorManageServiceImpl implements DoctorManageService {

    @Resource
    private DoctorManageMapper doctorManageMapper;

    @Resource
    private DeptInfoMapper deptInfoMapper;

    @Override
    public List<DoctorManageVo> listAll() {
        LambdaQueryWrapper<DoctorManage> qw = new LambdaQueryWrapper<>();
        qw.eq(DoctorManage::getIsDeleted, 0);
        qw.orderByDesc(DoctorManage::getCreateTime);
        List<DoctorManage> list = doctorManageMapper.selectList(qw);
        return toVoList(list);
    }

    @Override
    public DoctorManageVo getById(String doctorId) {
        DoctorManage doctor = doctorManageMapper.selectById(doctorId);
        if (doctor == null || (doctor.getIsDeleted() != null && doctor.getIsDeleted() == 1)) {
            throw new BusinessException("医生不存在");
        }
        return toVo(doctor);
    }

    @Override
    public void addDoctor(String name, Integer gender, String phone, String email,
                          String position, String goodAt, String introduction,
                          String departmentId, Integer doctorType) {
        if (doctorType == null || (doctorType != 1 && doctorType != 2 && doctorType != 3)) {
            throw new BusinessException("医生类型不合法");
        }

        // 校验手机号唯一性
        if (phone != null && !phone.isEmpty()) {
            LambdaQueryWrapper<DoctorManage> qw = new LambdaQueryWrapper<>();
            qw.eq(DoctorManage::getPhone, phone);
            qw.eq(DoctorManage::getIsDeleted, 0);
            if (doctorManageMapper.selectCount(qw) > 0) {
                throw new BusinessException("该手机号已被其他医生注册");
            }
        }

        DoctorManage doctor = new DoctorManage();
        doctor.setName(name);
        doctor.setGender(gender);
        doctor.setPhone(phone);
        doctor.setEmail(email);
        doctor.setPosition(position);
        doctor.setGoodAt(goodAt);
        doctor.setIntroduction(introduction);
        doctor.setDepartmentId(departmentId);
        doctor.setDoctorType(doctorType);
        doctor.setPassword("123456");
        doctor.setStatus(1);
        doctor.setIsDeleted(0);
        doctor.setCreateTime(new Date());

        doctorManageMapper.insert(doctor);
    }

    @Override
    public void updateDoctor(String doctorId, String name, Integer gender, String phone, String email,
                             String position, String goodAt, String introduction,
                             String departmentId, Integer doctorType, Integer status) {
        DoctorManage doctor = doctorManageMapper.selectById(doctorId);
        if (doctor == null || (doctor.getIsDeleted() != null && doctor.getIsDeleted() == 1)) {
            throw new BusinessException("医生不存在");
        }

        // 校验手机号唯一性（排除自己）
        if (phone != null && !phone.isEmpty() && !phone.equals(doctor.getPhone())) {
            LambdaQueryWrapper<DoctorManage> qw = new LambdaQueryWrapper<>();
            qw.eq(DoctorManage::getPhone, phone);
            qw.eq(DoctorManage::getIsDeleted, 0);
            qw.ne(DoctorManage::getDoctorId, doctorId);
            if (doctorManageMapper.selectCount(qw) > 0) {
                throw new BusinessException("该手机号已被其他医生注册");
            }
        }

        if (name != null) doctor.setName(name);
        if (gender != null) doctor.setGender(gender);
        if (phone != null) doctor.setPhone(phone);
        if (email != null) doctor.setEmail(email);
        if (position != null) doctor.setPosition(position);
        if (goodAt != null) doctor.setGoodAt(goodAt);
        if (introduction != null) doctor.setIntroduction(introduction);
        if (departmentId != null) doctor.setDepartmentId(departmentId);
        if (doctorType != null) {
            if (doctorType != 1 && doctorType != 2 && doctorType != 3) {
                throw new BusinessException("医生类型不合法");
            }
            doctor.setDoctorType(doctorType);
        }
        if (status != null) doctor.setStatus(status);

        doctorManageMapper.updateById(doctor);
    }

    @Override
    public void deleteDoctor(String doctorId) {
        DoctorManage doctor = doctorManageMapper.selectById(doctorId);
        if (doctor == null || (doctor.getIsDeleted() != null && doctor.getIsDeleted() == 1)) {
            throw new BusinessException("医生不存在");
        }
        doctor.setIsDeleted(1);
        doctor.setStatus(0);
        doctorManageMapper.updateById(doctor);
    }

    // ============ 私有辅助方法 ============

    private List<DoctorManageVo> toVoList(List<DoctorManage> list) {
        List<DoctorManageVo> vos = new ArrayList<>();
        for (DoctorManage d : list) {
            vos.add(toVo(d));
        }
        return vos;
    }

    private DoctorManageVo toVo(DoctorManage d) {
        DoctorManageVo vo = new DoctorManageVo();
        vo.setDoctorId(d.getDoctorId());
        vo.setName(d.getName());
        vo.setGender(d.getGender());
        vo.setPhone(d.getPhone());
        vo.setEmail(d.getEmail());
        vo.setPosition(d.getPosition());
        vo.setGoodAt(d.getGoodAt());
        vo.setIntroduction(d.getIntroduction());
        vo.setDepartmentId(d.getDepartmentId());
        vo.setStatus(d.getStatus());
        vo.setCreateTime(d.getCreateTime() != null ? d.getCreateTime().toString() : null);

        if (d.getDepartmentId() != null) {
            DeptInfo dept = deptInfoMapper.selectById(d.getDepartmentId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }
        return vo;
    }
}