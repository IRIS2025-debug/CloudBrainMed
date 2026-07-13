package com.cloudbrainmed.doctor.mapper;

import com.cloudbrainmed.doctor.entity.Prescription;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PrescriptionMapper {

    @Insert("INSERT INTO prescription (prescription_id, register_id, patient_id, doctor_id, " +
            "medicine_id, patient_name, doctor_name, medicine_name, spec, usage, num, create_date, " +
            "price, pay_status, create_time) " +
            "VALUES (#{prescriptionId}, #{registerId}, #{patientId}, #{doctorId}, " +
            "#{medicineId}, #{patientName}, #{doctorName}, #{medicineName}, #{spec}, #{usage}, #{num}, " +
            "#{createDate}, #{price}, 'WAITING', CURRENT_TIMESTAMP)")
    @Options(useGeneratedKeys = false)
    int insert(Prescription prescription);

    @Select("SELECT * FROM prescription WHERE register_id = #{registerId} ORDER BY create_time DESC")
    @Results({
        @Result(column = "prescription_id", property = "prescriptionId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "medicine_id", property = "medicineId"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "doctor_name", property = "doctorName"),
        @Result(column = "medicine_name", property = "medicineName"),
        @Result(column = "create_date", property = "createDate"),
        @Result(column = "pay_status", property = "payStatus"),
        @Result(column = "create_time", property = "createTime")
    })
    List<Prescription> selectByRegisterId(@Param("registerId") String registerId);

    @Select("SELECT * FROM prescription WHERE prescription_id = #{prescriptionId}")
    @Results({
        @Result(column = "prescription_id", property = "prescriptionId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "medicine_id", property = "medicineId"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "doctor_name", property = "doctorName"),
        @Result(column = "medicine_name", property = "medicineName"),
        @Result(column = "create_date", property = "createDate"),
        @Result(column = "pay_status", property = "payStatus"),
        @Result(column = "create_time", property = "createTime")
    })
    Prescription selectById(@Param("prescriptionId") String prescriptionId);
}
