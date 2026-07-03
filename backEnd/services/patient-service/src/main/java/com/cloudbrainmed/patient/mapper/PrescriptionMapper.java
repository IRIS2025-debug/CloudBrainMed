package com.cloudbrainmed.patient.mapper;

import com.cloudbrainmed.patient.entity.Prescription;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PrescriptionMapper {

    @Select("SELECT * FROM prescription WHERE register_id = #{registerId} AND patient_id = #{patientId}")
    @Results({
            @Result(column = "prescription_id", property = "prescriptionId"),
            @Result(column = "register_id", property = "registerId"),
            @Result(column = "patient_id", property = "patientId"),
            @Result(column = "doctor_id", property = "doctorId"),
            @Result(column = "medicine_id", property = "medicineId"),
            @Result(column = "patient_name", property = "patientName"),
            @Result(column = "doctor_name", property = "doctorName"),
            @Result(column = "medicine_name", property = "medicineName"),
            @Result(column = "spec", property = "spec"),
            @Result(column = "usage", property = "usage_"),
            @Result(column = "num", property = "num"),
            @Result(column = "prescription_date", property = "prescriptionDate"),
            @Result(column = "pay_status", property = "payStatus"),
            @Result(column = "create_time", property = "createTime")
    })
    List<Prescription> selectByRegisterId(@Param("registerId") String registerId,
                                          @Param("patientId") String patientId);


    @Select("SELECT p.* FROM prescription p INNER JOIN registration r ON p.register_id = r.register_id WHERE r.patient_id = #{patientId} ORDER BY p.create_time DESC")
    @Results({
        @Result(column = "prescription_id", property = "prescriptionId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "medicine_id", property = "medicineId"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "doctor_name", property = "doctorName"),
        @Result(column = "medicine_name", property = "medicineName"),
        @Result(column = "spec", property = "spec"),
        @Result(column = "usage", property = "usage_"),
        @Result(column = "num", property = "num"),
        @Result(column = "prescription_date", property = "prescriptionDate"),
        @Result(column = "pay_status", property = "payStatus"),
        @Result(column = "create_time", property = "createTime")
    })
    List<Prescription> selectByPatientId(@Param("patientId") String patientId);
}
