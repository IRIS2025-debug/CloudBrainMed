package com.cloudbrainmed.ai.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface AiFeedbackSampleMapper {

    @Select("""
        SELECT COUNT(*)
        FROM ai_feedback_sample
        WHERE trace_id = #{traceId}
          AND doctor_id = #{doctorId}
        """)
    int countByTraceIdAndDoctorId(
            @Param("traceId") String traceId,
            @Param("doctorId") String doctorId);

    @Insert("""
        INSERT INTO ai_feedback_sample (
            sample_id, trace_id, ai_output_json, final_output_json,
            is_adopted, diff_score, label_tag, used_for_training,
            created_at, doctor_id
        ) VALUES (
            #{sampleId}, #{traceId}, CAST(#{aiOutputJson} AS JSON),
            CAST(#{finalOutputJson} AS JSON), #{isAdopted}, #{diffScore},
            'ASSISTED_CONSULT', 0, #{createdAt}, #{doctorId}
        )
        """)
    int insert(
            @Param("sampleId") String sampleId,
            @Param("traceId") String traceId,
            @Param("aiOutputJson") String aiOutputJson,
            @Param("finalOutputJson") String finalOutputJson,
            @Param("isAdopted") short isAdopted,
            @Param("diffScore") BigDecimal diffScore,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("doctorId") String doctorId);
}
