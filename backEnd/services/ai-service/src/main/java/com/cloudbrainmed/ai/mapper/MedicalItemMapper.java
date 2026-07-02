package com.cloudbrainmed.ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface MedicalItemMapper {

    @Select("SELECT item_id, item_code, item_name, item_category, dept_id, price " +
            "FROM medical_item WHERE status = 1 ORDER BY item_category, item_code")
    List<Map<String, Object>> selectEnabled();

    @Select("SELECT item_id, item_code, item_name, item_category " +
            "FROM medical_item WHERE item_name = #{itemName} AND status = 1")
    Map<String, Object> selectByName(String itemName);
}