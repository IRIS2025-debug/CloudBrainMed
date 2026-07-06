package com.cloudbrainmed.patient.service;

import com.cloudbrainmed.patient.dto.RegisterSubmitDto;
import com.cloudbrainmed.patient.entity.Dept;
import com.cloudbrainmed.patient.entity.Doctor;
import com.cloudbrainmed.patient.entity.Registration;
import com.cloudbrainmed.patient.vo.DoctorDetailVo;
import com.cloudbrainmed.patient.vo.ScheduleVo;
import com.cloudbrainmed.patient.vo.VisitDetail;

import java.util.List;
import java.util.Map;

public interface RegisterService {
    List<Dept> getAllDepts();
    List<Doctor> getDoctorsByDept(String deptId);
    DoctorDetailVo getDoctorDetail(String doctorId);
    List<ScheduleVo> getDoctorSchedules(String doctorId);
    Registration submitRegister(RegisterSubmitDto dto);
    List<Registration> getRegisterHistory(String patientId);
    Registration getRegisterDetail(String registerId);

    /**
     * 检查患者信息是否完整
     * @param patientId 患者ID
     * @return 是否完整及缺失字段列表
     */
    Map<String, Object> checkPatientInfo(String patientId);
    VisitDetail getVisitDetail(String registerId);

}