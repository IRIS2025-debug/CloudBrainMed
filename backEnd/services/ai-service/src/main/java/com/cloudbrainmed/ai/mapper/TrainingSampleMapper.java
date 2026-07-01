package com.cloudbrainmed.ai.mapper;

import com.cloudbrainmed.ai.entity.TrainingSample;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TrainingSampleMapper {

    @Insert("""
            INSERT INTO ai_feedback_sample (
                sample_id, trace_id, final_output_json, label_tag, used_for_training, created_at
            ) VALUES (
                #{sampleId}, #{datasetName}, CAST(#{label} AS json), #{labelType}, 0, #{createTime}
            )
            """)
    int insert(TrainingSample sample);

    @Select("""
            SELECT sample_id,
                   trace_id AS dataset_name,
                   final_output_json::text AS label,
                   label_tag AS label_type,
                   CASE WHEN used_for_training = 1 THEN 'TRAINED' ELSE 'LABELED' END AS status,
                   created_at AS create_time
            FROM ai_feedback_sample
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    @Results({
        @Result(column = "sample_id", property = "sampleId"),
        @Result(column = "dataset_name", property = "datasetName"),
        @Result(column = "file_path", property = "filePath"),
        @Result(column = "label_type", property = "labelType"),
        @Result(column = "create_time", property = "createTime")
    })
    List<TrainingSample> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    @Update("""
            UPDATE ai_feedback_sample
            SET label_tag = #{label},
                used_for_training = CASE WHEN #{status} = 'TRAINED' THEN 1 ELSE used_for_training END
            WHERE sample_id = #{sampleId}
            """)
    int updateLabel(@Param("sampleId") String sampleId, @Param("label") String label,
                    @Param("labelType") String labelType, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM ai_feedback_sample")
    int countAll();

    @Select("SELECT COUNT(*) FROM ai_feedback_sample WHERE is_adopted = 1 OR used_for_training = 1")
    int countAdopted();
}
