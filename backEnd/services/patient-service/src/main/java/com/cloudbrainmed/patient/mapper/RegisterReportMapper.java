package com.cloudbrainmed.patient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudbrainmed.patient.entity.RegisterReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RegisterReportMapper extends BaseMapper<RegisterReport> {

    /**
     * 根据挂号ID查询病历报告
     */
    @Select("SELECT * FROM register_report WHERE register_id = #{registerId}")
    RegisterReport selectByRegisterId(@Param("registerId") String registerId);

    /**
     * 根据患者ID查询最新的病历报告
     */
    @Select("SELECT * FROM register_report WHERE patient_id = #{patientId} ORDER BY create_time DESC LIMIT 1")
    RegisterReport selectLatestByPatientId(@Param("patientId") String patientId);
}