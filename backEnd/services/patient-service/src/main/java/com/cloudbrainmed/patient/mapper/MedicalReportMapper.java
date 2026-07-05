// src/main/java/com/cloudbrainmed/patient/mapper/MedicalReportMapper.java
package com.cloudbrainmed.patient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudbrainmed.patient.entity.MedicalReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MedicalReportMapper extends BaseMapper<MedicalReport> {

    @Select("SELECT * FROM medical_report WHERE report_id = #{reportId}")
    MedicalReport selectByReportId(@Param("reportId") String reportId);

    @Select("SELECT * FROM medical_report WHERE order_item_id = #{orderItemId}")
    MedicalReport selectByOrderItemId(@Param("orderItemId") String orderItemId);

    @Select("SELECT * FROM medical_report WHERE order_item_id IN (${orderItemIds})")
    List<MedicalReport> selectByOrderItemIds(@Param("orderItemIds") List<String> orderItemIds);

    @Select("SELECT * FROM medical_report WHERE patient_id = #{patientId}")
    List<MedicalReport> selectByPatientId(@Param("patientId") String patientId);
}