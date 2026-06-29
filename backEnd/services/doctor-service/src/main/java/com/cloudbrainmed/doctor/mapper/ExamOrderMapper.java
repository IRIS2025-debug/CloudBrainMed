package com.cloudbrainmed.doctor.mapper;

import com.cloudbrainmed.doctor.entity.ExamOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ExamOrderMapper {

    @Select("SELECT mo.order_id, mo.patient_id, mo.register_id, mo.doctor_id, " +
            "p.name AS patient_name, p.gender, EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birthday)) AS age, " +
            "moi.item_category, mi.item_name, moi.price, " +
            "mo.pay_status, mo.urgency_level, mo.create_time " +
            "FROM medical_order mo " +
            "JOIN patient p ON mo.patient_id = p.patient_id " +
            "JOIN medical_order_item moi ON mo.order_id = moi.order_id " +
            "LEFT JOIN medical_item mi ON moi.item_id = mi.item_id " +
            "WHERE mo.register_id = #{registerId} AND mo.doctor_id = #{doctorId}")
    @Results({
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "item_category", property = "itemCategory"),
        @Result(column = "item_name", property = "itemName"),
        @Result(column = "pay_status", property = "payStatus"),
        @Result(column = "urgency_level", property = "urgencyLevel"),
        @Result(column = "create_time", property = "createTime")
    })
    List<ExamOrder> selectByRegisterId(
            @Param("registerId") String registerId,
            @Param("doctorId") String doctorId);

    @Select("SELECT mo.order_id, mo.patient_id, mo.register_id, mo.doctor_id, " +
            "p.name AS patient_name, p.gender, EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birthday)) AS age, " +
            "moi.item_category, mi.item_name, moi.price, " +
            "mo.pay_status, mo.urgency_level, mo.create_time " +
            "FROM medical_order mo " +
            "JOIN patient p ON mo.patient_id = p.patient_id " +
            "JOIN medical_order_item moi ON mo.order_id = moi.order_id " +
            "LEFT JOIN medical_item mi ON moi.item_id = mi.item_id " +
            "WHERE mo.patient_id = #{patientId} ORDER BY mo.create_time DESC")
    @Results({
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "item_category", property = "itemCategory"),
        @Result(column = "item_name", property = "itemName"),
        @Result(column = "pay_status", property = "payStatus"),
        @Result(column = "urgency_level", property = "urgencyLevel"),
        @Result(column = "create_time", property = "createTime")
    })
    List<ExamOrder> selectByPatientId(@Param("patientId") String patientId);

    @Select("SELECT mo.order_id, mo.patient_id, mo.register_id, mo.doctor_id, " +
            "p.name AS patient_name, p.gender, EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birthday)) AS age, " +
            "moi.item_category, mi.item_name, moi.price, " +
            "mo.pay_status, mo.urgency_level, mo.create_time " +
            "FROM medical_order mo " +
            "JOIN patient p ON mo.patient_id = p.patient_id " +
            "JOIN medical_order_item moi ON mo.order_id = moi.order_id " +
            "LEFT JOIN medical_item mi ON moi.item_id = mi.item_id " +
            "WHERE mo.doctor_id = #{doctorId} ORDER BY mo.create_time DESC")
    @Results({
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "item_category", property = "itemCategory"),
        @Result(column = "item_name", property = "itemName"),
        @Result(column = "pay_status", property = "payStatus"),
        @Result(column = "urgency_level", property = "urgencyLevel"),
        @Result(column = "create_time", property = "createTime")
    })
    List<ExamOrder> selectByDoctorId(@Param("doctorId") String doctorId);
}
