package com.cloudbrainmed.doctor.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface MedicalRecordMapper {

    /** 按患者 ID 查询历史病历记录（供 AI 上下文组装用） */
    @Select("SELECT m.record_id, m.patient_id, m.doctor_id, m.register_id, " +
            "m.doctor_name, m.patient_name, m.visit_age, m.description, " +
            "m.visit_date, m.pay_status " +
            "FROM register_report m " +
            "WHERE m.patient_id = #{patientId} " +
            "ORDER BY m.visit_date DESC")
    List<Map<String, Object>> selectByPatientId(@Param("patientId") String patientId);
}