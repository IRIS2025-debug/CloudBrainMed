package com.cloudbrainmed.ai.mapper;

import com.cloudbrainmed.ai.entity.ModelVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ModelVersionMapper {

    @Insert("INSERT INTO ai_model_version (model_id, model_key, model_type, version, artifact_path, hyper_params, status, create_time) " +
            "VALUES (#{modelId}, #{modelKey}, #{modelType}, #{version}, #{artifactPath}, #{hyperParams}, #{status}, #{createTime})")
    int insert(ModelVersion mv);

    @Select("SELECT * FROM ai_model_version WHERE model_id = #{modelId}")
    @Results({
        @Result(column = "model_id", property = "modelId"),
        @Result(column = "model_key", property = "modelKey"),
        @Result(column = "model_type", property = "modelType"),
        @Result(column = "artifact_path", property = "artifactPath"),
        @Result(column = "hyper_params", property = "hyperParams"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "activated_at", property = "activatedAt")
    })
    ModelVersion selectById(@Param("modelId") String modelId);

    @Select("SELECT * FROM ai_model_version WHERE status = 'ACTIVE' ORDER BY create_time DESC LIMIT 1")
    @Results({
        @Result(column = "model_id", property = "modelId"),
        @Result(column = "model_key", property = "modelKey"),
        @Result(column = "model_type", property = "modelType"),
        @Result(column = "artifact_path", property = "artifactPath"),
        @Result(column = "hyper_params", property = "hyperParams"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "activated_at", property = "activatedAt")
    })
    ModelVersion selectActive();

    @Select("SELECT * FROM ai_model_version ORDER BY create_time DESC")
    @Results({
        @Result(column = "model_id", property = "modelId"),
        @Result(column = "model_key", property = "modelKey"),
        @Result(column = "model_type", property = "modelType"),
        @Result(column = "artifact_path", property = "artifactPath"),
        @Result(column = "hyper_params", property = "hyperParams"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "activated_at", property = "activatedAt")
    })
    List<ModelVersion> selectAll();

    @Update("UPDATE ai_model_version SET status = #{status} WHERE model_id = #{modelId}")
    int updateStatus(@Param("modelId") String modelId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM ai_model_version")
    int countAll();

    @Select("SELECT COUNT(*) FROM ai_model_version WHERE status = #{status}")
    int countByStatus(@Param("status") String status);
}
