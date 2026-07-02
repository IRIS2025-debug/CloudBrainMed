package com.cloudbrainmed.doctor.mapper;

import com.cloudbrainmed.doctor.entity.MedicalOrder;
import com.cloudbrainmed.doctor.entity.MedicalOrderItem;
import com.cloudbrainmed.doctor.vo.InspectionOrderVo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MedicalOrderMapper {

    @Insert("""
        INSERT INTO medical_order (
            order_id, patient_id, register_id, doctor_id, clinical_summary,
            urgency_level, source_type, ai_trace_id, status, pay_status,
            assigned_room, confirmed_time, create_time
        ) VALUES (
            #{orderId}, #{patientId}, #{registerId}, #{doctorId},
            #{clinicalSummary}, #{urgencyLevel}, #{sourceType}, #{aiTraceId},
            #{status}, #{payStatus}, #{assignedRoom}, #{confirmedTime}, #{createTime}
        )
        """)
    int insertOrder(MedicalOrder order);

    @Insert("""
        INSERT INTO medical_order_item (
            order_item_id, order_id, item_id, item_code, item_name,
            item_category, assigned_dept_id, urgency_level, price,
            status, create_time
        ) VALUES (
            #{orderItemId}, #{orderId}, #{itemId}, #{itemCode}, #{itemName},
            #{itemCategory}, #{assignedDeptId}, #{urgencyLevel}, #{price},
            #{status}, #{createTime}
        )
        """)
    int insertOrderItem(MedicalOrderItem item);

    @Select("""
        SELECT input_summary
        FROM ai_inference_log
        WHERE trace_id = #{traceId}
          AND patient_id = #{patientId}
          AND call_source = 'AI_EXAM_RECOMMEND'
          AND status = 'SUCCESS'
        ORDER BY create_time DESC
        LIMIT 1
        """)
    String findAiRecommendationInput(
            @Param("traceId") String traceId,
            @Param("patientId") String patientId);

    @Select("""
        SELECT output_summary
        FROM ai_inference_log
        WHERE trace_id = #{traceId}
          AND patient_id = #{patientId}
          AND call_source = 'AI_EXAM_RECOMMEND'
          AND status = 'SUCCESS'
        ORDER BY create_time DESC
        LIMIT 1
        """)
    String findAiRecommendationOutput(
            @Param("traceId") String traceId,
            @Param("patientId") String patientId);

    @Update("""
        UPDATE registration
        SET consult_status = CASE
            WHEN consult_status = 'PENDING' THEN 'IN_PROGRESS'
            ELSE consult_status
        END
        WHERE register_id = #{registerId}
          AND doctor_id = #{doctorId}
          AND consult_status IS DISTINCT FROM 'COMPLETED'
        """)
    int keepConsultInProgress(
            @Param("registerId") String registerId,
            @Param("doctorId") String doctorId);

    /** 检验医生视角：查看所有检验类（LAB）医技申请 */
    @Select("SELECT mo.order_id, mo.patient_id, p.name AS patient_name, p.gender, " +
            "EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birthday)) AS age, " +
            "mo.register_id, mo.doctor_id, mo.clinical_summary, " +
            "mi.item_name, mi.item_code, moi.item_category, " +
            "mo.urgency_level, mo.source_type, mo.status, mo.pay_status, mo.assigned_room, " +
            "mo.confirmed_time::timestamp AS confirmed_time, mo.create_time::timestamp AS create_time " +
            "FROM medical_order mo " +
            "JOIN patient p ON mo.patient_id = p.patient_id " +
            "JOIN medical_order_item moi ON mo.order_id = moi.order_id " +
            "LEFT JOIN medical_item mi ON moi.item_id = mi.item_id " +
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
        @Result(column = "assigned_room", property = "assignedRoom"),
        @Result(column = "confirmed_time", property = "confirmedTime"),
        @Result(column = "create_time", property = "createTime")
    })
    List<InspectionOrderVo> selectAllLabOrders();

    /** 按订单ID查单条检验申请详情 */
    @Select("SELECT order_id, patient_id, register_id, doctor_id, clinical_summary, urgency_level, " +
            "source_type, ai_trace_id, status, pay_status, assigned_room, confirmed_time::timestamp AS confirmed_time, " +
            "create_time::timestamp AS create_time, update_time::timestamp AS update_time " +
            "FROM medical_order WHERE order_id = #{orderId}")
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
        @Result(column = "assigned_room", property = "assignedRoom"),
        @Result(column = "confirmed_time", property = "confirmedTime"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "update_time", property = "updateTime")
    })
    MedicalOrder selectByOrderId(@Param("orderId") String orderId);

    @Update("""
        UPDATE medical_order
        SET status = 'QUEUED',
            assigned_room = #{assignedRoom}
        WHERE order_id = #{orderId}
          AND status = 'WAITING_ASSIGN'
          AND pay_status = 'PAID'
        """)
    int assignOrder(@Param("orderId") String orderId,
                    @Param("assignedRoom") String assignedRoom);
}
