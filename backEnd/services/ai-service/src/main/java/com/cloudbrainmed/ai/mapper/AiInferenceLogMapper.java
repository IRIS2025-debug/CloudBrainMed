package com.cloudbrainmed.ai.mapper;

import com.cloudbrainmed.ai.entity.AiInferenceLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiInferenceLogMapper {

    @Insert("""
        INSERT INTO ai_inference_log (
            log_id, trace_id, call_source, model_key, model_version,
            input_summary, output_summary, status, duration_ms, created_at, patient_id
        ) VALUES (
            #{logId}, #{traceId}, #{callSource}, #{modelKey}, #{modelVersion},
            #{inputSummary}, #{outputSummary}, #{status}, #{durationMs}, #{createdAt}, #{patientId}
        )
        """)
    int insert(AiInferenceLog log);

    @Select("SELECT * FROM ai_inference_log ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    @Results({
        @Result(column = "log_id", property = "logId"),
        @Result(column = "trace_id", property = "traceId"),
        @Result(column = "call_source", property = "callSource"),
        @Result(column = "model_key", property = "modelKey"),
        @Result(column = "model_version", property = "modelVersion"),
        @Result(column = "input_summary", property = "inputSummary"),
        @Result(column = "output_summary", property = "outputSummary"),
        @Result(column = "duration_ms", property = "durationMs"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "patient_id", property = "patientId")
    })
    List<AiInferenceLog> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM ai_inference_log")
    int countAll();

    @Select("SELECT COUNT(*) FROM ai_inference_log WHERE created_at >= CURRENT_DATE")
    int countToday();

    @Select("SELECT COALESCE(AVG(duration_ms), 0) FROM ai_inference_log WHERE status = 'SUCCESS'")
    double avgLatency();

}
