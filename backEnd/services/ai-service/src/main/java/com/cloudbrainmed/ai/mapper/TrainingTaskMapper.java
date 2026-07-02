package com.cloudbrainmed.ai.mapper;

import com.cloudbrainmed.ai.entity.TrainingTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TrainingTaskMapper {

    @Insert("INSERT INTO ai_training_task " +
            "(task_id, model_key, model_type, hyper_params, dataset_path, status, model_id, error_message, create_time, start_time, completed_time) " +
            "VALUES (#{taskId}, #{modelKey}, #{modelType}, #{hyperParams}, #{datasetPath}, #{status}, #{modelId}, #{errorMessage}, #{createTime}, #{startTime}, #{completedTime})")
    int insert(TrainingTask task);

    @Update("UPDATE ai_training_task SET status = #{status}, model_id = #{modelId}, error_message = #{errorMessage}, " +
            "start_time = #{startTime}, completed_time = #{completedTime} WHERE task_id = #{taskId}")
    int updateStatus(TrainingTask task);

    @Select("SELECT * FROM ai_training_task ORDER BY create_time DESC")
    @Results({
        @Result(column = "task_id", property = "taskId"),
        @Result(column = "model_key", property = "modelKey"),
        @Result(column = "model_type", property = "modelType"),
        @Result(column = "hyper_params", property = "hyperParams"),
        @Result(column = "dataset_path", property = "datasetPath"),
        @Result(column = "model_id", property = "modelId"),
        @Result(column = "error_message", property = "errorMessage"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "start_time", property = "startTime"),
        @Result(column = "completed_time", property = "completedTime")
    })
    List<TrainingTask> selectAll();
}
