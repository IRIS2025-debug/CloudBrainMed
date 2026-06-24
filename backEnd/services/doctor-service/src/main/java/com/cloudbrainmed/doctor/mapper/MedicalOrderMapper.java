package com.cloudbrainmed.doctor.mapper;

import com.cloudbrainmed.doctor.entity.MedicalOrder;
import com.cloudbrainmed.doctor.entity.MedicalOrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MedicalOrderMapper {

    @Insert("""
        INSERT INTO medical_order (
            order_id, patient_id, register_id, doctor_id, clinical_summary,
            urgency_level, source_type, ai_trace_id, status, pay_status,
            confirmed_time, create_time
        ) VALUES (
            #{orderId}, #{patientId}, #{registerId}, #{doctorId},
            #{clinicalSummary}, #{urgencyLevel}, #{sourceType}, #{aiTraceId},
            #{status}, #{payStatus}, #{confirmedTime}, #{createTime}
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
        ORDER BY created_at DESC
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
        ORDER BY created_at DESC
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
}
