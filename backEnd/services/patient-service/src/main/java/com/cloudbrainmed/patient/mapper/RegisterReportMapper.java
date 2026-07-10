package com.cloudbrainmed.patient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudbrainmed.patient.entity.RegisterReport;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RegisterReportMapper extends BaseMapper<RegisterReport> {

    @Select("SELECT * FROM register_report WHERE register_id = #{registerId} AND patient_id = #{patientId}")
    @Results({
            @Result(column = "record_id", property = "recordId"),
            @Result(column = "patient_id", property = "patientId"),
            @Result(column = "doctor_id", property = "doctorId"),
            @Result(column = "register_id", property = "registerId"),
            @Result(column = "doctor_name", property = "doctorName"),
            @Result(column = "patient_name", property = "patientName"),
            @Result(column = "visit_age", property = "visitAge"),
            @Result(column = "visit_date", property = "visitDate"),
            @Result(column = "pay_status", property = "payStatus"),
            @Result(column = "create_time", property = "createTime")
    })
    List<RegisterReport> selectByRegisterId(@Param("registerId") String registerId,
                                           @Param("patientId") String patientId);

    @Select("SELECT m.* FROM register_report m INNER JOIN registration r ON m.register_id = r.register_id WHERE r.patient_id = #{patientId} ORDER BY m.create_time DESC")
    @Results({
            @Result(column = "record_id", property = "recordId"),
            @Result(column = "patient_id", property = "patientId"),
            @Result(column = "doctor_id", property = "doctorId"),
            @Result(column = "register_id", property = "registerId"),
            @Result(column = "doctor_name", property = "doctorName"),
            @Result(column = "patient_name", property = "patientName"),
            @Result(column = "visit_age", property = "visitAge"),
            @Result(column = "visit_date", property = "visitDate"),
            @Result(column = "pay_status", property = "payStatus"),
            @Result(column = "create_time", property = "createTime")
    })
    List<RegisterReport> selectByPatientId(@Param("patientId") String patientId);

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