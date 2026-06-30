package com.cloudbrainmed.ai.mapper;

import com.cloudbrainmed.ai.entity.TrainingSample;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TrainingSampleMapper {

    @Insert("INSERT INTO training_sample (sample_id, dataset_name, file_path, label, label_type, status, create_time) " +
            "VALUES (#{sampleId}, #{datasetName}, #{filePath}, #{label}, #{labelType}, #{status}, #{createTime})")
    int insert(TrainingSample sample);

    @Select("SELECT * FROM training_sample ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    @Results({
        @Result(column = "sample_id", property = "sampleId"),
        @Result(column = "dataset_name", property = "datasetName"),
        @Result(column = "file_path", property = "filePath"),
        @Result(column = "label_type", property = "labelType"),
        @Result(column = "create_time", property = "createTime")
    })
    List<TrainingSample> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    @Update("UPDATE training_sample SET label = #{label}, label_type = #{labelType}, status = #{status} WHERE sample_id = #{sampleId}")
    int updateLabel(@Param("sampleId") String sampleId, @Param("label") String label,
                    @Param("labelType") String labelType, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM training_sample")
    int countAll();

    @Select("SELECT COUNT(*) FROM training_sample WHERE status IN ('LABELED', 'TRAINED')")
    int countAdopted();
}
