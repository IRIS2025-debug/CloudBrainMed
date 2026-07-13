package com.cloudbrainmed.doctor.mapper;

import com.cloudbrainmed.doctor.entity.ConsultRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface ConsultMapper {

    @Select("<script>" +
        "SELECT r.register_id, r.patient_id, r.doctor_id, r.name, r.gender, r.birthday, " +
        "r.chief_complaint, r.department, r.consult_room, r.visit_date, r.consult_time, " +
        "r.price, r.pay_status, r.consult_status, r.create_time, " +
        "EXTRACT(YEAR FROM AGE(NOW(), r.birthday)) AS patient_age, " +
        "(SELECT COUNT(*) FROM medical_order mo " +
        " JOIN medical_order_item moi ON moi.order_id = mo.order_id " +
        " JOIN medical_report mr ON mr.order_item_id = moi.order_item_id " +
        " WHERE mo.register_id = r.register_id AND mr.status = 'PUBLISHED') AS report_count, " +
        "(SELECT MAX(mr.report_time)::timestamp FROM medical_order mo " +
        " JOIN medical_order_item moi ON moi.order_id = mo.order_id " +
        " JOIN medical_report mr ON mr.order_item_id = moi.order_item_id " +
        " WHERE mo.register_id = r.register_id AND mr.status = 'PUBLISHED') AS latest_report_time " +
        "FROM registration r " +
        "WHERE r.doctor_id = #{doctorId} AND r.pay_status = 'PAID' " +
        "<if test='consultStatus != null and consultStatus != \"\"'>AND r.consult_status = #{consultStatus}</if> " +
        "<if test='date != null and date != \"\"'>AND r.visit_date = #{date}::date</if> " +
        "<if test='reportReturnedOnly'>AND EXISTS (SELECT 1 FROM medical_order mo " +
        " JOIN medical_order_item moi ON moi.order_id = mo.order_id " +
        " JOIN medical_report mr ON mr.order_item_id = moi.order_item_id " +
        " WHERE mo.register_id = r.register_id AND mr.status = 'PUBLISHED')</if> " +
        "ORDER BY r.create_time DESC LIMIT #{limit} OFFSET #{offset}" +
        "</script>")
    @Results({
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "chief_complaint", property = "chiefComplaint"),
        @Result(column = "consult_room", property = "consultRoom"),
        @Result(column = "visit_date", property = "visitDate"),
        @Result(column = "consult_time", property = "consultTime"),
        @Result(column = "pay_status", property = "payStatus"),
        @Result(column = "consult_status", property = "consultStatus"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "patient_age", property = "patientAge"),
        @Result(column = "report_count", property = "reportCount"),
        @Result(column = "latest_report_time", property = "latestReportTime")
    })
    List<ConsultRecord> findList(@Param("doctorId") String doctorId,
                                  @Param("consultStatus") String consultStatus,
                                  @Param("date") String date,
                                  @Param("reportReturnedOnly") boolean reportReturnedOnly,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    @Select("SELECT r.*, m.record_id, COALESCE(m.doctor_name, d.name) AS doctor_name, " +
        "m.patient_name, m.visit_age, m.description, m.create_time AS record_create_time, " +
        "EXTRACT(YEAR FROM AGE(NOW(), r.birthday)) AS patient_age " +
        "FROM registration r LEFT JOIN register_report m ON r.register_id = m.register_id " +
        "LEFT JOIN doctor d ON r.doctor_id = d.doctor_id " +
        "WHERE r.register_id = #{registerId}")
    @Results({
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "chief_complaint", property = "chiefComplaint"),
        @Result(column = "consult_room", property = "consultRoom"),
        @Result(column = "visit_date", property = "visitDate"),
        @Result(column = "consult_time", property = "consultTime"),
        @Result(column = "pay_status", property = "payStatus"),
        @Result(column = "consult_status", property = "consultStatus"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "record_id", property = "recordId"),
        @Result(column = "doctor_name", property = "doctorName"),
        @Result(column = "patient_name", property = "patientName"),
        @Result(column = "visit_age", property = "visitAge"),
        @Result(column = "patient_age", property = "patientAge")
    })
    ConsultRecord findDetail(@Param("registerId") String registerId);

    @Select("SELECT record_id FROM register_report WHERE register_id = #{registerId}")
    String findRecordId(@Param("registerId") String registerId);

    @Select("SELECT name FROM doctor WHERE doctor_id = #{doctorId}")
    String findDoctorName(@Param("doctorId") String doctorId);

    @Insert("INSERT INTO register_report (record_id, patient_id, doctor_id, register_id, doctor_name, patient_name, visit_age, description, visit_date, pay_status, create_time) " +
        "VALUES (#{recordId}, #{patientId}, #{doctorId}, #{registerId}, #{doctorName}, #{patientName}, #{visitAge}, #{description}, #{visitDate}, #{payStatus}, #{createTime})")
    int insertRecord(ConsultRecord r);

    @Update("UPDATE register_report SET description=#{description} WHERE register_id=#{registerId}")
    int updateRecordDesc(@Param("registerId") String registerId, @Param("description") String description);

    @Update("UPDATE registration SET consult_status='IN_PROGRESS' WHERE register_id=#{registerId}")
    int markInProgress(@Param("registerId") String registerId);

    @Update("UPDATE registration SET consult_status='RECORD_CONFIRMED' WHERE register_id=#{registerId}")
    int markRecordConfirmed(@Param("registerId") String registerId);

    @Insert("INSERT INTO medical_order (order_id, patient_id, register_id, doctor_id, clinical_summary, urgency_level, source_type, status, pay_status, create_time) " +
        "VALUES (#{orderId}, #{patientId}, #{registerId}, #{doctorId}, #{clinicalSummary}, #{urgencyLevel}, 'MANUAL', 'WAITING_ASSIGN', 'WAITING', NOW())")
    int insertCheckReport(@Param("orderId") String orderId,
                           @Param("patientId") String patientId,
                           @Param("registerId") String registerId,
                           @Param("doctorId") String doctorId,
                           @Param("clinicalSummary") String clinicalSummary,
                           @Param("urgencyLevel") String urgencyLevel);

    /** 按项目名称列表批量查 medical_item 字典 */
    @Select("<script>" +
            "SELECT item_id, item_code, item_name, item_category, price, dept_id " +
            "FROM medical_item WHERE item_name IN " +
            "<foreach collection='itemNames' item='name' open='(' separator=',' close=')'>" +
            "#{name}" +
            "</foreach>" +
            " AND status = 1" +
            "</script>")
    List<Map<String, Object>> findMedicalItemsByNames(@Param("itemNames") List<String> itemNames);

    /** 写入 medical_order_item 子表行 */
    @Insert("INSERT INTO medical_order_item (order_item_id, order_id, item_id, item_code, item_name, item_category, assigned_dept_id, urgency_level, price, status, create_time) " +
            "VALUES (#{orderItemId}, #{orderId}, #{itemId}, #{itemCode}, #{itemName}, #{itemCategory}, #{assignedDeptId}, #{urgencyLevel}, #{price}, 'WAITING_ASSIGN', NOW())")
    int insertOrderItem(@Param("orderItemId") String orderItemId,
                        @Param("orderId") String orderId,
                        @Param("itemId") String itemId,
                        @Param("itemCode") String itemCode,
                        @Param("itemName") String itemName,
                        @Param("itemCategory") String itemCategory,
                        @Param("assignedDeptId") String assignedDeptId,
                        @Param("urgencyLevel") String urgencyLevel,
                        @Param("price") BigDecimal price);

    @Update("UPDATE registration SET consult_status='COMPLETED' WHERE register_id=#{registerId}")
    int completeConsult(@Param("registerId") String registerId);

    @Select("""
        SELECT description
        FROM register_report
        WHERE patient_id = #{patientId}
          AND register_id <> #{registerId}
          AND description IS NOT NULL
          AND description <> ''
        ORDER BY visit_date DESC, create_time DESC
        LIMIT 5
        """)
    List<String> findMedicalHistory(
            @Param("patientId") String patientId,
            @Param("registerId") String registerId);

    @Select("""
        SELECT CONCAT_WS('；',
            NULLIF(moi.item_name, ''),
            NULLIF(mr.result_summary, ''),
            NULLIF(mr.conclusion, ''),
            CASE WHEN mr.abnormal_flag <> 'NORMAL'
                 THEN '异常标记：' || mr.abnormal_flag END
        )
        FROM medical_report mr
        JOIN medical_order_item moi
          ON moi.order_item_id = mr.order_item_id
        JOIN medical_order mo
          ON mo.order_id = moi.order_id
        WHERE mr.patient_id = #{patientId}
          AND (mo.register_id IS NULL OR mo.register_id <> #{registerId})
          AND mr.status = 'PUBLISHED'
        ORDER BY mr.report_time DESC NULLS LAST,
                 mr.performed_time DESC NULLS LAST,
                 mr.create_time DESC
        LIMIT 5
        """)
    List<String> findPreviousReports(
            @Param("patientId") String patientId,
            @Param("registerId") String registerId);

    /**
     * 接诊医生首页概览：仅统计当前医生「当天」接诊记录，按 consult_status 分组计数。
     * 首页四张指标卡由此结果聚合，不额外读写任何医技订单表。
     */
    @Select("""
        SELECT r.consult_status AS consult_status, COUNT(*) AS cnt
        FROM registration r
        WHERE r.doctor_id = #{doctorId}
          AND r.visit_date = CURRENT_DATE
        GROUP BY r.consult_status
        """)
    List<Map<String, Object>> countTodayByStatus(@Param("doctorId") String doctorId);

    /**
     * 接诊医生首页概览：当前医生「当天」最近若干条接诊记录，用于首页底部动态。
     * 按接诊时间、创建时间倒序，只读 registration 视图字段。
     */
    @Select("""
        SELECT r.register_id, r.patient_id, r.doctor_id, r.name, r.gender,
               r.chief_complaint, r.department, r.consult_room, r.visit_date,
               r.consult_time, r.consult_status, r.create_time,
               EXTRACT(YEAR FROM AGE(NOW(), r.birthday)) AS patient_age
        FROM registration r
        WHERE r.doctor_id = #{doctorId}
          AND r.visit_date = CURRENT_DATE
        ORDER BY r.consult_time DESC NULLS LAST, r.create_time DESC
        LIMIT #{limit}
        """)
    @Results({
        @Result(column = "register_id", property = "registerId"),
        @Result(column = "patient_id", property = "patientId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "chief_complaint", property = "chiefComplaint"),
        @Result(column = "consult_room", property = "consultRoom"),
        @Result(column = "visit_date", property = "visitDate"),
        @Result(column = "consult_time", property = "consultTime"),
        @Result(column = "consult_status", property = "consultStatus"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "patient_age", property = "patientAge")
    })
    List<ConsultRecord> findRecentToday(@Param("doctorId") String doctorId,
                                        @Param("limit") int limit);
}
