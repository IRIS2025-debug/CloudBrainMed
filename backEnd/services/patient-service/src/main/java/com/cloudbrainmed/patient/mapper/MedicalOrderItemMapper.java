// src/main/java/com/cloudbrainmed/patient/mapper/MedicalOrderItemMapper.java
package com.cloudbrainmed.patient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudbrainmed.patient.entity.MedicalOrderItem;
import com.cloudbrainmed.patient.vo.RoomInfoVo;
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

    @Select("SELECT COUNT(1) FROM medical_order_item WHERE item_id = #{itemId} AND status = 'QUEUED'")
    Integer countQueuedByItemId(@Param("itemId") String itemId);

    @Select("SELECT estimated_duration_min FROM medical_room_item WHERE item_id = #{itemId} LIMIT 1")
    Integer selectEstimatedDurationMinByItemId(@Param("itemId") String itemId);

    @Select("SELECT room_id AS roomId, room_name AS roomName FROM medical_room WHERE operator = #{operator}")
    List<RoomInfoVo> selectRoomsByOperator(@Param("operator") String operator);
}
