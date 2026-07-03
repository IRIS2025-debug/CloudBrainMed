// src/main/java/com/cloudbrainmed/patient/mapper/MedicalOrderMapper.java
package com.cloudbrainmed.patient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudbrainmed.patient.entity.MedicalOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MedicalOrderMapper extends BaseMapper<MedicalOrder> {

    @Select("SELECT * FROM medical_order WHERE order_id = #{orderId}")
    MedicalOrder selectByOrderId(@Param("orderId") String orderId);

    @Select("SELECT * FROM medical_order WHERE patient_id = #{patientId}")
    List<MedicalOrder> selectByPatientId(@Param("patientId") String patientId);

    @Select("SELECT * FROM medical_order WHERE register_id = #{registerId}")
    List<MedicalOrder> selectByRegisterId(@Param("registerId") String registerId);
}