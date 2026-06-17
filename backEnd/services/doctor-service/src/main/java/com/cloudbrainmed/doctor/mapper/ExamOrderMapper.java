package com.cloudbrainmed.doctor.mapper;

import com.cloudbrainmed.doctor.entity.ExamOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ExamOrderMapper {

    @Select("SELECT * FROM check_report WHERE register_id = #{registerId}")
    @Results({
        @Result(column = "report_id", property = "reportId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "check_type", property = "checkType"),
        @Result(column = "check_item", property = "checkItem"),
        @Result(column = "pay_status", property = "payStatus"),
        @Result(column = "urgency_level", property = "urgencyLevel"),
        @Result(column = "create_time", property = "createTime")
    })
    List<ExamOrder> selectByRegisterId(@Param("registerId") String registerId);

    @Select("SELECT * FROM check_report WHERE patient_id = #{patientId} ORDER BY create_time DESC")
    @Results({
        @Result(column = "report_id", property = "reportId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "check_type", property = "checkType"),
        @Result(column = "check_item", property = "checkItem"),
        @Result(column = "pay_status", property = "payStatus"),
        @Result(column = "urgency_level", property = "urgencyLevel"),
        @Result(column = "create_time", property = "createTime")
    })
    List<ExamOrder> selectByPatientId(@Param("patientId") String patientId);

    @Select("SELECT * FROM check_report WHERE doctor_id = #{doctorId} ORDER BY create_time DESC")
    @Results({
        @Result(column = "report_id", property = "reportId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "check_type", property = "checkType"),
        @Result(column = "check_item", property = "checkItem"),
        @Result(column = "pay_status", property = "payStatus"),
        @Result(column = "urgency_level", property = "urgencyLevel"),
        @Result(column = "create_time", property = "createTime")
    })
    List<ExamOrder> selectByDoctorId(@Param("doctorId") String doctorId);
}
