// src/main/java/com/cloudbrainmed/patient/mapper/MedicalOrderItemMapper.java
package com.cloudbrainmed.patient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudbrainmed.patient.entity.MedicalOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MedicalOrderItemMapper extends BaseMapper<MedicalOrderItem> {
    @Select("SELECT * FROM medical_order_item WHERE order_item_id = #{orderItemId}")
    MedicalOrderItem selectByOrderItemId(@Param("orderItemId") String orderItemId);

    @Select("SELECT * FROM medical_order_item WHERE order_id = #{orderId}")
    List<MedicalOrderItem> selectByOrderId(@Param("orderId") String orderId);

    @Select("SELECT moi.* FROM medical_order_item moi " +
            "INNER JOIN medical_order mo ON moi.order_id = mo.order_id " +
            "WHERE mo.patient_id = #{patientId}")
    List<MedicalOrderItem> selectByPatientId(@Param("patientId") String patientId);

    @Select("SELECT moi.* FROM medical_order_item moi " +
            "INNER JOIN medical_order mo ON moi.order_id = mo.order_id " +
            "WHERE mo.register_id = #{registerId}")
    List<MedicalOrderItem> selectByRegisterId(@Param("registerId") String registerId);
}