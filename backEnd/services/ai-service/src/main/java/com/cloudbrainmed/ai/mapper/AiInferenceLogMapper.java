package com.cloudbrainmed.ai.mapper;

import com.cloudbrainmed.ai.entity.AiInferenceLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiInferenceLogMapper {

    @Insert("INSERT INTO ai_inference_log (log_id, inference_type, status, model_id, latency_ms, message, create_time) " +
            "VALUES (#{logId}, #{inferenceType}, #{status}, #{modelId}, #{latencyMs}, #{message}, #{createTime})")
    int insert(AiInferenceLog log);

    @Select("SELECT * FROM ai_inference_log ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    @Results({
        @Result(column = "log_id", property = "logId"),
        @Result(column = "inference_type", property = "inferenceType"),
        @Result(column = "model_id", property = "modelId"),
        @Result(column = "latency_ms", property = "latencyMs"),
        @Result(column = "create_time", property = "createTime")
    })
    List<AiInferenceLog> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM ai_inference_log")
    int countAll();

    @Select("SELECT COUNT(*) FROM ai_inference_log WHERE create_time >= CURRENT_DATE")
    int countToday();

    @Select("SELECT COALESCE(AVG(latency_ms), 0) FROM ai_inference_log WHERE status = 'SUCCESS'")
    double avgLatency();
}
