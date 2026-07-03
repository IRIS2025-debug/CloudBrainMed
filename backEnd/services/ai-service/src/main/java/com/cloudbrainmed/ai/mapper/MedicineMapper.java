package com.cloudbrainmed.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudbrainmed.ai.entity.Medicine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MedicineMapper extends BaseMapper<Medicine> {

    @Select("""
            SELECT * FROM medicine
            WHERE name LIKE CONCAT('%', #{keyword}, '%')
               OR #{keyword} LIKE CONCAT('%', name, '%')
            LIMIT 1
            """)
    Medicine findByKeyword(@Param("keyword") String keyword);

    @Select("SELECT * FROM medicine")
    List<Medicine> selectAll();
}
