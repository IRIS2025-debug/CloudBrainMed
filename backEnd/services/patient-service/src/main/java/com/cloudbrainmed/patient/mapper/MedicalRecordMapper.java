package com.cloudbrainmed.patient.mapper;

import com.cloudbrainmed.patient.entity.MedicalRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MedicalRecordMapper {

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
    List<MedicalRecord> selectByRegisterId(@Param("registerId") String registerId,
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
    List<MedicalRecord> selectByPatientId(@Param("patientId") String patientId);
}
