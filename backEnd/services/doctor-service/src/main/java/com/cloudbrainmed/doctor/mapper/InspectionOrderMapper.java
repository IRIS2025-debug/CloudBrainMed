package com.cloudbrainmed.doctor.mapper;

import com.cloudbrainmed.doctor.entity.InspectionOrder;
import com.cloudbrainmed.doctor.vo.InspectionOrderVo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InspectionOrderMapper {

    /** 检验医生视角：查看所有检验类（LAB）医技申请 */
    @Select("SELECT mo.order_id, mo.patient_id, p.name AS patient_name, p.gender, " +
            "EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birthday)) AS age, " +
            "mo.register_id, mo.doctor_id, mo.clinical_summary, " +
            "mi.item_name, mi.item_code, moi.item_category, " +
            "mo.urgency_level, mo.source_type, mo.status, mo.pay_status, " +
            "mo.confirmed_time, mo.create_time " +
            "FROM medical_order mo " +
            "JOIN patient p ON mo.patient_id = p.patient_id " +
            "JOIN medical_order_item moi ON mo.order_id = moi.order_id " +
            "LEFT JOIN medical_item mi ON moi.item_id = mi.item_id " +
            "WHERE moi.item_category = 'LAB' " +
            "ORDER BY mo.create_time DESC")
    @Results({
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "gender", property = "gender"),
        @Result(column = "age", property = "age"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "clinical_summary", property = "clinicalSummary"),
        @Result(column = "item_name", property = "itemName"),
        @Result(column = "item_code", property = "itemCode"),
        @Result(column = "item_category", property = "itemCategory"),
        @Result(column = "urgency_level", property = "urgencyLevel"),
        @Result(column = "source_type", property = "sourceType"),
        @Result(column = "status", property = "status"),
        @Result(column = "pay_status", property = "payStatus"),
        @Result(column = "confirmed_time", property = "confirmedTime"),
        @Result(column = "create_time", property = "createTime")
    })
    List<InspectionOrderVo> selectAllLabOrders();

    /** 按订单ID查单条检验申请详情 */
    @Select("SELECT * FROM medical_order WHERE order_id = #{orderId}")
    @Results({
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "clinical_summary", property = "clinicalSummary"),
        @Result(column = "urgency_level", property = "urgencyLevel"),
        @Result(column = "source_type", property = "sourceType"),
        @Result(column = "ai_trace_id", property = "aiTraceId"),
        @Result(column = "status", property = "status"),
        @Result(column = "pay_status", property = "payStatus"),
        @Result(column = "confirmed_time", property = "confirmedTime"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "update_time", property = "updateTime")
    })
    InspectionOrder selectByOrderId(@Param("orderId") String orderId);
}
