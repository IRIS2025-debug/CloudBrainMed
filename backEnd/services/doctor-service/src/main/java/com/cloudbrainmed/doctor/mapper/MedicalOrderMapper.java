package com.cloudbrainmed.doctor.mapper;

import com.cloudbrainmed.doctor.entity.MedicalOrder;
import com.cloudbrainmed.doctor.entity.MedicalOrderItem;
import com.cloudbrainmed.doctor.vo.InspectionOrderVo;
import lombok.Data;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    MedicalOrder selectByOrderId(@Param("orderId") String orderId);

    /**
     * 更新订单支付状态
     */
    @Update("""
        UPDATE medical_order SET pay_status = #{payStatus}, update_time = NOW()
        WHERE order_id = #{orderId}
        """)
    int updatePayStatus(@Param("orderId") String orderId, @Param("payStatus") String payStatus);

    /**
     * 查询医生当前正在处理的任务数量
     */
    @Select("""
        SELECT COUNT(*) FROM medical_order_item moi
        JOIN medical_order mo ON moi.order_id = mo.order_id
        WHERE moi.assigned_doctor_id = #{doctorId} AND moi.status = 'IN_PROCESS'
        """)
    int countInProgressByDoctor(@Param("doctorId") String doctorId);

    /**
     * 查询患者当前是否有正在处理的任务
     */
    @Select("""
        SELECT COUNT(*) FROM medical_order_item moi
        JOIN medical_order mo ON moi.order_id = mo.order_id
        WHERE mo.patient_id = #{patientId} AND moi.status = 'IN_PROCESS'
        """)
    int countInProgressByPatient(@Param("patientId") String patientId);

    /**
     * 获取待调度的排队任务（按优先级排序）
     * 返回 queued 状态，按 urgency_level (紧急优先) + create_time 排序
     */
    @Select("""
        SELECT moi.order_item_id, moi.order_id, moi.item_code, moi.item_name,
               moi.item_category, moi.urgency_level, moi.price,
               moi.status, moi.create_time,
               mo.patient_id, mo.register_id, mo.doctor_id AS requester_doctor_id,
               p.name AS patient_name, p.gender,
               EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birthday)) AS age
        FROM medical_order_item moi
        JOIN medical_order mo ON moi.order_id = mo.order_id
        JOIN patient p ON mo.patient_id = p.patient_id
        WHERE moi.status = 'QUEUED' AND mo.pay_status = 'PAID'
        ORDER BY
            CASE moi.urgency_level
                WHEN 'EMERGENCY' THEN 1
                WHEN 'URGENT' THEN 2
                WHEN 'NORMAL' THEN 3
                ELSE 4
            END,
            moi.create_time ASC
        LIMIT #{limit}
        """)
    @Results({
        @Result(column = "order_item_id", property = "orderItemId"),
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "item_code", property = "itemCode"),
        @Result(column = "item_name", property = "itemName"),
        @Result(column = "item_category", property = "itemCategory"),
        @Result(column = "urgency_level", property = "urgencyLevel"),
        @Result(column = "price", property = "price"),
        @Result(column = "status", property = "status"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "requester_doctor_id", property = "requesterDoctorId"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "gender", property = "gender"),
        @Result(column = "age", property = "age")
    })
    List<QueuedTaskItem> findQueuedTasks(@Param("limit") int limit);

    /**
     * 更新检查项目状态
     */
    @Update("""
        UPDATE medical_order_item SET status = #{newStatus}
        WHERE order_item_id = #{orderItemId} AND status = #{oldStatus}
        """)
    int updateItemStatus(@Param("orderItemId") String orderItemId,
                         @Param("oldStatus") String oldStatus,
                         @Param("newStatus") String newStatus);

    /**
     * 更新检查项目状态为已完成，并设置完成时间
     */
    @Update("""
        UPDATE medical_order_item SET status = 'COMPLETED', complete_time = NOW()
        WHERE order_item_id = #{orderItemId} AND status = 'IN_PROCESS' AND assigned_doctor_id = #{doctorId}
        """)
    int completeTask(@Param("orderItemId") String orderItemId, @Param("doctorId") String doctorId);

    /**
     * 分配医生给检查项目
     */
    @Update("""
        UPDATE medical_order_item SET assigned_doctor_id = #{doctorId}, assign_time = NOW()
        WHERE order_item_id = #{orderItemId}
        """)
    int assignDoctor(@Param("orderItemId") String orderItemId, @Param("doctorId") String doctorId);

    /**
     * 查询医生当前队列（IN_PROCESS 状态的任务）
     * 按医生类型过滤：检查医生(2)只看EXAM，检验医生(3)只看LAB
     */
    @Select("""
        <script>
        SELECT moi.order_item_id, moi.order_id, moi.item_code, moi.item_name,
               moi.item_category, moi.urgency_level, moi.price,
               moi.status, moi.create_time, moi.assign_time,
               mo.patient_id, mo.register_id,
               p.name AS patient_name, p.gender,
               EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birthday)) AS age,
               mo.clinical_summary
        FROM medical_order_item moi
        JOIN medical_order mo ON moi.order_id = mo.order_id
        JOIN patient p ON mo.patient_id = p.patient_id
        WHERE moi.assigned_doctor_id = #{doctorId}
        <if test="itemCategory != null and itemCategory != ''">
          AND moi.item_category = #{itemCategory}
        </if>
        ORDER BY moi.create_time DESC
        </script>
        """)
    @Results({
        @Result(column = "order_item_id", property = "orderItemId"),
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "item_code", property = "itemCode"),
        @Result(column = "item_name", property = "itemName"),
        @Result(column = "item_category", property = "itemCategory"),
        @Result(column = "urgency_level", property = "urgencyLevel"),
        @Result(column = "price", property = "price"),
        @Result(column = "status", property = "status"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "assign_time", property = "assignTime"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "gender", property = "gender"),
        @Result(column = "age", property = "age"),
        @Result(column = "clinical_summary", property = "clinicalSummary")
    })
    List<DoctorTaskVo> selectDoctorTasks(@Param("doctorId") String doctorId, @Param("itemCategory") String itemCategory);

    /**
     * 根据ID查询任务详情
     */
    @Select("""
        SELECT moi.order_item_id, moi.order_id, moi.item_code, moi.item_name,
               moi.item_category, moi.urgency_level, moi.price,
               moi.status, moi.create_time, moi.assign_time, moi.complete_time,
               mo.patient_id, mo.register_id, mo.doctor_id AS requester_doctor_id,
               p.name AS patient_name, p.gender, p.birthday,
               EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birthday)) AS age,
               mo.clinical_summary
        FROM medical_order_item moi
        JOIN medical_order mo ON moi.order_id = mo.order_id
        JOIN patient p ON mo.patient_id = p.patient_id
        WHERE moi.order_item_id = #{orderItemId}
        """)
    @Results({
        @Result(column = "order_item_id", property = "orderItemId"),
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "item_code", property = "itemCode"),
        @Result(column = "item_name", property = "itemName"),
        @Result(column = "item_category", property = "itemCategory"),
        @Result(column = "urgency_level", property = "urgencyLevel"),
        @Result(column = "price", property = "price"),
        @Result(column = "status", property = "status"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "assign_time", property = "assignTime"),
        @Result(column = "complete_time", property = "completeTime"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "requester_doctor_id", property = "requesterDoctorId"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "gender", property = "gender"),
        @Result(column = "birthday", property = "birthday"),
        @Result(column = "age", property = "age"),
        @Result(column = "clinical_summary", property = "clinicalSummary")
    })
    DoctorTaskDetailVo selectTaskDetailById(@Param("orderItemId") String orderItemId);

    /**
     * 更新订单支付状态（幂等：只有 WAITING 状态才能更新为 PAID）
     */
    @Update("""
        UPDATE medical_order SET pay_status = 'PAID'
        WHERE order_id = #{orderId} AND pay_status = 'WAITING'
        """)
    int updatePayStatus(@Param("orderId") String orderId);

    /**
     * 批量将订单下所有检查项目从 waiting_assign 转为 queued
     */
    @Update("""
        UPDATE medical_order_item SET status = 'QUEUED'
        WHERE order_id = #{orderId} AND status = 'WAITING_ASSIGN'
        """)
    int enqueueOrderItems(@Param("orderId") String orderId);

    /**
     * 统计排队中的任务数
     */
    @Select("""
        SELECT COUNT(*) FROM medical_order_item moi
        JOIN medical_order mo ON moi.order_id = mo.order_id
        WHERE moi.status = 'QUEUED' AND mo.pay_status = 'PAID'
        """)
    long countQueuedTasks();

    @Data
    class QueuedTaskItem {
        private String orderItemId;
        private String orderId;
        private String itemCode;
        private String itemName;
        private String itemCategory;
        private String urgencyLevel;
        private BigDecimal price;
        private String status;
        private LocalDateTime createTime;
        private String patientId;
        private String registerId;
        private String requesterDoctorId;
        private String patientName;
        private Integer gender;
        private Integer age;
    }

    @Data
    class DoctorTaskVo {
        private String orderItemId;
        private String orderId;
        private String itemCode;
        private String itemName;
        private String itemCategory;
        private String urgencyLevel;
        private BigDecimal price;
        private String status;
        private LocalDateTime createTime;
        private LocalDateTime assignTime;
        private String patientId;
        private String registerId;
        private String patientName;
        private Integer gender;
        private Integer age;
        private String clinicalSummary;
    }

    @Data
    class DoctorTaskDetailVo {
        private String orderItemId;
        private String orderId;
        private String itemCode;
        private String itemName;
        private String itemCategory;
        private String urgencyLevel;
        private BigDecimal price;
        private String status;
        private LocalDateTime createTime;
        private LocalDateTime assignTime;
        private LocalDateTime completeTime;
        private String patientId;
        private String registerId;
        private String requesterDoctorId;
        private String patientName;
        private Integer gender;
        private LocalDate birthday;
        private Integer age;
        private String clinicalSummary;
    }
}