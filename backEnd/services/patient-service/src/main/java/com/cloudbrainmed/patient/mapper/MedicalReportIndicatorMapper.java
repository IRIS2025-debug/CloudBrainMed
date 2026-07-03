// src/main/java/com/cloudbrainmed/patient/mapper/MedicalReportIndicatorMapper.java
package com.cloudbrainmed.patient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudbrainmed.patient.entity.MedicalReportIndicator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MedicalReportIndicatorMapper extends BaseMapper<MedicalReportIndicator> {

    @Select("SELECT * FROM medical_report_indicator WHERE report_id = #{reportId} ORDER BY sort_no ASC")
    List<MedicalReportIndicator> selectByReportId(@Param("reportId") String reportId);
}