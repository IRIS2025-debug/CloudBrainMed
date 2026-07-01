package com.cloudbrainmed.ai.mapper;

import com.cloudbrainmed.ai.entity.ModelVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ModelVersionMapper {

    @Insert("INSERT INTO ai_model_registry (model_id, model_key, version, artifact_path, status, traffic_pct, created_at) " +
            "VALUES (#{modelId}, #{modelKey}, #{version}, #{artifactPath}, #{status}, 0, #{createTime})")
    int insert(ModelVersion mv);

    @Select("SELECT * FROM ai_model_registry WHERE model_id = #{modelId}")
    @Results({
        @Result(column = "model_id", property = "modelId"),
        @Result(column = "model_key", property = "modelKey"),
        @Result(column = "artifact_path", property = "artifactPath"),
        @Result(column = "created_at", property = "createTime")
    })
    ModelVersion selectById(@Param("modelId") String modelId);

    @Select("SELECT * FROM ai_model_registry WHERE status = 'ACTIVE' ORDER BY created_at DESC LIMIT 1")
    @Results({
        @Result(column = "model_id", property = "modelId"),
        @Result(column = "model_key", property = "modelKey"),
        @Result(column = "artifact_path", property = "artifactPath"),
        @Result(column = "created_at", property = "createTime")
    })
    ModelVersion selectActive();

    @Select("SELECT * FROM ai_model_registry ORDER BY created_at DESC")
    @Results({
        @Result(column = "model_id", property = "modelId"),
        @Result(column = "model_key", property = "modelKey"),
        @Result(column = "artifact_path", property = "artifactPath"),
        @Result(column = "created_at", property = "createTime")
    })
    List<ModelVersion> selectAll();

    @Update("UPDATE ai_model_registry SET status = #{status}, update_time = CURRENT_TIMESTAMP WHERE model_id = #{modelId}")
    int updateStatus(@Param("modelId") String modelId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM ai_model_registry")
    int countAll();

    @Select("SELECT COUNT(*) FROM ai_model_registry WHERE status = #{status}")
    int countByStatus(@Param("status") String status);
}
