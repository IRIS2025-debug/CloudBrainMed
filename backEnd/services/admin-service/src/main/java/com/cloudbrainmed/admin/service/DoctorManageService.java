package com.cloudbrainmed.admin.service;

import com.cloudbrainmed.admin.vo.DoctorManageVo;

import java.util.List;

public interface DoctorManageService {
    /** 查询所有医生列表（未删除的） */
    List<DoctorManageVo> listAll();

    /** 根据ID查询 */
    DoctorManageVo getById(String doctorId);

    /** 新增医生 */
    void addDoctor(String name, Integer gender, String phone, String email,
                   String position, String goodAt, String introduction, String departmentId);

    /** 修改医生信息 */
    void updateDoctor(String doctorId, String name, Integer gender, String phone, String email,
                      String position, String goodAt, String introduction, String departmentId, Integer status);

    /** 删除医生（软删除） */
    void deleteDoctor(String doctorId);
}