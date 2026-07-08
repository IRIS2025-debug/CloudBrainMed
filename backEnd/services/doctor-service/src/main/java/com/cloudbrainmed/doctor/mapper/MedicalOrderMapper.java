package com.cloudbrainmed.doctor.mapper;

import com.cloudbrainmed.doctor.entity.MedicalOrder;
import com.cloudbrainmed.doctor.entity.MedicalOrderItem;
import com.cloudbrainmed.doctor.entity.MedicalReport;
import com.cloudbrainmed.doctor.vo.InspectionOrderVo;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;
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
            assigned_room, confirmed_time, create_time
        ) VALUES (
            #{orderId}, #{patientId}, #{registerId}, #{doctorId},
            #{clinicalSummary}, #{urgencyLevel}, #{sourceType}, #{aiTraceId},
            #{status}, #{payStatus}, #{assignedRoom}, #{confirmedTime}, #{createTime}
        )
        """)
    int insertOrder(MedicalOrder order);

    /**
     * 原子认领任务：同时更新状态和分配医生
     * 返回 1 表示认领成功，0 表示状态不对
     */
    @Update("""
    UPDATE medical_order_item
    SET status = 'IN_PROCESS',
        assigned_doctor_id = #{doctorId},
        assign_time = NOW()
    WHERE order_item_id = #{orderItemId} AND status = 'QUEUED'
    """)
    int claimTaskAtomically(@Param("orderItemId") String orderItemId,
                            @Param("doctorId") String doctorId);

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
    @Select("SELECT moi.order_item_id, mo.order_id, mo.patient_id, p.name AS patient_name, p.gender, " +
            "EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birthday)) AS age, " +
            "mo.register_id, mo.doctor_id, mo.clinical_summary, " +
            "mi.item_name, mi.item_code, moi.item_category, " +
            "mo.urgency_level, mo.source_type, moi.status, mo.pay_status, " +
            "mo.assigned_room, moi.assigned_doctor_id, " +
            "mo.confirmed_time::timestamp AS confirmed_time, mo.create_time::timestamp AS create_time " +
            "FROM medical_order mo " +
            "JOIN patient p ON mo.patient_id = p.patient_id " +
            "JOIN medical_order_item moi ON mo.order_id = moi.order_id " +
            "LEFT JOIN medical_item mi ON moi.item_id = mi.item_id " +
            "ORDER BY mo.create_time DESC")
    @Results({
        @Result(column = "order_item_id", property = "orderItemId"),
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
        @Result(column = "assigned_doctor_id", property = "assignedDoctorId"),
        @Result(column = "confirmed_time", property = "confirmedTime"),
        @Result(column = "create_time", property = "createTime")
    })
    List<InspectionOrderVo> selectAllLabOrders();

    @Select("""
        SELECT mo.order_id, mo.patient_id, mo.register_id, mo.doctor_id,
               mo.clinical_summary, mo.urgency_level, mo.source_type,
               mo.ai_trace_id, mo.status, mo.pay_status, mo.assigned_room,
               mo.confirmed_time::timestamp AS confirmed_time,
               mo.create_time::timestamp AS create_time,
               mo.update_time::timestamp AS update_time
        FROM medical_order mo
        JOIN medical_order_item moi ON moi.order_id = mo.order_id
        WHERE moi.order_item_id = #{orderItemId}
        """)
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
        @Result(column = "assigned_doctor_id", property = "assignedDoctorId"),
        @Result(column = "confirmed_time", property = "confirmedTime"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "update_time", property = "updateTime")
    })
    MedicalOrder selectByOrderItemId(@Param("orderItemId") String orderItemId);

    @Update("""
        UPDATE medical_order
        SET status = CASE
                WHEN status = 'WAITING_ASSIGN' THEN 'QUEUED'
                ELSE status
            END,
            assigned_room = #{assignedRoom}
        WHERE order_id = #{orderId}
          AND pay_status = 'PAID'
        """)
    int assignOrderRoom(@Param("orderId") String orderId,
                        @Param("assignedRoom") String assignedRoom);

    @Update("""
        UPDATE medical_order_item moi
        SET status = 'QUEUED',
            update_time = NOW()
        FROM medical_order mo
        WHERE moi.order_id = mo.order_id
          AND moi.order_item_id = #{orderItemId}
          AND moi.status = 'WAITING_ASSIGN'
          AND mo.pay_status = 'PAID'
        """)
    int enqueueOrderItemForAssignment(@Param("orderItemId") String orderItemId);

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
     * 获取待调度的排队任务（按优先级排序，带时效老化防饥饿）
     * 排序规则：
     * 1. 紧急 > 加急 > 常规
     * 2. 同级别按创建时间升序
     * 3. 常规任务排队超过 30 分钟自动升级为加急级别
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
          CASE
            WHEN moi.urgency_level = 'EMERGENCY' THEN 1
            WHEN moi.urgency_level = 'URGENT' THEN 2
            WHEN moi.urgency_level = 'NORMAL' AND moi.create_time < NOW() - INTERVAL '30 minutes' THEN 2
            WHEN moi.urgency_level = 'NORMAL' THEN 3
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
        WHERE moi.status = 'QUEUED'
          AND mo.pay_status = 'PAID'
          AND moi.item_category = #{itemCategory}
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
    List<QueuedTaskItem> findQueuedTasksByCategory(
            @Param("itemCategory") String itemCategory,
            @Param("limit") int limit);

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
    /**
     * 根据ID查询任务详情
     */
    @Select("""
        SELECT moi.order_item_id, moi.order_id, moi.item_code, moi.item_name,
               moi.item_category, moi.urgency_level, moi.price,
               moi.status, moi.create_time, moi.assign_time, moi.complete_time,
               moi.assigned_doctor_id,
               mo.patient_id, mo.register_id, mo.doctor_id AS requester_doctor_id,
               rd.name AS requester_doctor_name,
               dept.dept_name AS assigned_dept_name,
               p.name AS patient_name, p.gender, p.birthday,
               EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birthday)) AS age,
               mo.clinical_summary
        FROM medical_order_item moi
        JOIN medical_order mo ON moi.order_id = mo.order_id
        JOIN patient p ON mo.patient_id = p.patient_id
        LEFT JOIN doctor rd ON mo.doctor_id = rd.doctor_id
        LEFT JOIN department dept ON moi.assigned_dept_id = dept.dept_id
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
        @Result(column = "assigned_doctor_id", property = "assignedDoctorId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "requester_doctor_id", property = "requesterDoctorId"),
        @Result(column = "requester_doctor_name", property = "requesterDoctorName"),
        @Result(column = "assigned_dept_name", property = "assignedDeptName"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "gender", property = "gender"),
        @Result(column = "birthday", property = "birthday"),
        @Result(column = "age", property = "age"),
        @Result(column = "clinical_summary", property = "clinicalSummary")
    })
    DoctorTaskDetailVo selectTaskDetailById(@Param("orderItemId") String orderItemId);

    @Insert("""
        INSERT INTO medical_report (
            report_id, order_item_id, patient_id, item_category,
            result_summary, conclusion, abnormal_flag, attachment_url, ai_result_json,
            report_doctor_id, status, performed_time, report_time,
            create_time, update_time
        ) VALUES (
            #{reportId}, #{orderItemId}, #{patientId}, #{itemCategory},
            #{resultSummary}, #{conclusion}, #{abnormalFlag}, #{attachmentUrl}, #{aiResultJson},
            #{reportDoctorId}, #{status}, #{performedTime}, #{reportTime},
            #{createTime}, #{updateTime}
        )
        ON CONFLICT (order_item_id) DO UPDATE SET
            result_summary = EXCLUDED.result_summary,
            conclusion = EXCLUDED.conclusion,
            abnormal_flag = EXCLUDED.abnormal_flag,
            attachment_url = EXCLUDED.attachment_url,
            ai_result_json = EXCLUDED.ai_result_json,
            report_doctor_id = EXCLUDED.report_doctor_id,
            status = EXCLUDED.status,
            performed_time = EXCLUDED.performed_time,
            report_time = EXCLUDED.report_time,
            update_time = EXCLUDED.update_time
        """)
    int insertMedicalReport(MedicalReport report);

    @Select("""
        SELECT mr.report_id, mr.order_item_id, moi.order_id, mo.register_id,
               mr.patient_id, moi.item_code, moi.item_name, mr.item_category,
               mr.result_summary, mr.conclusion, mr.abnormal_flag,
               mr.attachment_url, mr.ai_result_json, mr.report_doctor_id, mr.status,
               mr.performed_time::timestamp AS performed_time,
               mr.report_time::timestamp AS report_time
        FROM medical_report mr
        JOIN medical_order_item moi ON moi.order_item_id = mr.order_item_id
        JOIN medical_order mo ON mo.order_id = moi.order_id
        WHERE mo.register_id = #{registerId}
          AND mr.status = 'PUBLISHED'
        ORDER BY mr.report_time DESC NULLS LAST, mr.create_time DESC
        """)
    @Results({
        @Result(column = "report_id", property = "reportId"),
        @Result(column = "order_item_id", property = "orderItemId"),
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "item_code", property = "itemCode"),
        @Result(column = "item_name", property = "itemName"),
        @Result(column = "item_category", property = "itemCategory"),
        @Result(column = "result_summary", property = "resultSummary"),
        @Result(column = "conclusion", property = "conclusion"),
        @Result(column = "abnormal_flag", property = "abnormalFlag"),
        @Result(column = "attachment_url", property = "attachmentUrl"),
        @Result(column = "ai_result_json", property = "aiResultJson"),
        @Result(column = "report_doctor_id", property = "reportDoctorId"),
        @Result(column = "status", property = "status"),
        @Result(column = "performed_time", property = "performedTime"),
        @Result(column = "report_time", property = "reportTime")
    })
    List<MedicalReportVo> findPublishedReportsByRegisterId(
            @Param("registerId") String registerId);

    @Select("""
        SELECT mr.report_id, mr.order_item_id, moi.order_id, mo.register_id,
               mr.patient_id, moi.item_code, moi.item_name, mr.item_category,
               mr.result_summary, mr.conclusion, mr.abnormal_flag,
               mr.attachment_url, mr.ai_result_json, mr.report_doctor_id, mr.status,
               mr.performed_time::timestamp AS performed_time,
               mr.report_time::timestamp AS report_time
        FROM medical_report mr
        JOIN medical_order_item moi ON moi.order_item_id = mr.order_item_id
        JOIN medical_order mo ON mo.order_id = moi.order_id
        WHERE mr.order_item_id = #{orderItemId}
          AND mr.status = 'PUBLISHED'
        ORDER BY mr.report_time DESC NULLS LAST, mr.create_time DESC
        LIMIT 1
        """)
    @Results({
        @Result(column = "report_id", property = "reportId"),
        @Result(column = "order_item_id", property = "orderItemId"),
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "item_code", property = "itemCode"),
        @Result(column = "item_name", property = "itemName"),
        @Result(column = "item_category", property = "itemCategory"),
        @Result(column = "result_summary", property = "resultSummary"),
        @Result(column = "conclusion", property = "conclusion"),
        @Result(column = "abnormal_flag", property = "abnormalFlag"),
        @Result(column = "attachment_url", property = "attachmentUrl"),
        @Result(column = "ai_result_json", property = "aiResultJson"),
        @Result(column = "report_doctor_id", property = "reportDoctorId"),
        @Result(column = "status", property = "status"),
        @Result(column = "performed_time", property = "performedTime"),
        @Result(column = "report_time", property = "reportTime")
    })
    MedicalReportVo findPublishedReportByOrderItemId(
            @Param("orderItemId") String orderItemId);

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
     * 支付成功后将申请主表从待分配推进到已排队。
     */
    @Update("""
        UPDATE medical_order SET status = 'QUEUED', update_time = NOW()
        WHERE order_id = #{orderId}
          AND status = 'WAITING_ASSIGN'
          AND pay_status = 'PAID'
        """)
    int enqueueOrder(@Param("orderId") String orderId);

    /**
     * 统计排队中的任务数
     */
    @Select("""
        SELECT COUNT(*) FROM medical_order_item moi
        JOIN medical_order mo ON moi.order_id = mo.order_id
        WHERE moi.status = 'QUEUED' AND mo.pay_status = 'PAID'
        """)
    long countQueuedTasks();

    @Select("""
        SELECT COUNT(*) FROM medical_order_item moi
        JOIN medical_order mo ON moi.order_id = mo.order_id
        WHERE moi.status = 'QUEUED'
          AND mo.pay_status = 'PAID'
          AND moi.item_category = #{itemCategory}
        """)
    long countQueuedTasksByCategory(@Param("itemCategory") String itemCategory);

    @Select("""
        SELECT COUNT(*) FROM medical_report
        WHERE order_item_id = #{orderItemId}
          AND status = 'PUBLISHED'
        """)
    long countPublishedReportsByOrderItemId(
            @Param("orderItemId") String orderItemId);

    /**
     * 直接更新项目状态（不校验旧状态）
     */
    @Update("""
        UPDATE medical_order_item SET status = #{status}
        WHERE order_item_id = #{orderItemId}
        """)
    int updateItemStatusDirect(@Param("orderItemId") String orderItemId,
                               @Param("status") String status);

    /**
     * 释放任务：将 IN_PROCESS 状态的任务回退到 QUEUED，清除医生分配
     * 用于医生跳过任务场景
     */
    /**
     * 根据ID查询order_item
     */
    @Select("""
        SELECT order_item_id, order_id, item_id, item_code, item_name,
               item_category, assigned_dept_id, assigned_doctor_id,
               urgency_level, price, status,
               create_time, assign_time, complete_time
        FROM medical_order_item
        WHERE order_item_id = #{orderItemId}
        """)
    @Results({
        @Result(column = "order_item_id", property = "orderItemId"),
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "item_id", property = "itemId"),
        @Result(column = "item_code", property = "itemCode"),
        @Result(column = "item_name", property = "itemName"),
        @Result(column = "item_category", property = "itemCategory"),
        @Result(column = "assigned_dept_id", property = "assignedDeptId"),
        @Result(column = "assigned_doctor_id", property = "assignedDoctorId"),
        @Result(column = "urgency_level", property = "urgencyLevel"),
        @Result(column = "price", property = "price"),
        @Result(column = "status", property = "status"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "assign_time", property = "assignTime"),
        @Result(column = "complete_time", property = "completeTime")
    })
    com.cloudbrainmed.doctor.entity.MedicalOrderItem selectOrderItemById(
            @Param("orderItemId") String orderItemId);


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
        private String assignedDoctorId;
        private String patientId;
        private String registerId;
        private String requesterDoctorId;
        private String requesterDoctorName;
        private String assignedDeptName;
        private String patientName;
        private Integer gender;
        private LocalDate birthday;
        private Integer age;
        private String clinicalSummary;
    }
}
